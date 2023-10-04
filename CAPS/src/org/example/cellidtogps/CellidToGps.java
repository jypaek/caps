/**
 * CellidToGps
 * - Main Application for CAPS Algorithm
 *   (CAPS: Cell-ID Aided Postioning System)
 *
 * @author      Jeongyeup Paek (jpaek@usc.edu)
 * @modified    Aug. 3, 2010
 **/
package org.example.cellidtogps;

import java.io.File;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import android.app.Activity;

import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Environment;

import android.content.Context;
import android.content.Intent;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import android.telephony.TelephonyManager;
import android.telephony.PhoneStateListener;
import android.telephony.CellLocation;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.telephony.ServiceState;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import android.widget.TextView;

public class CellidToGps extends Activity implements LogEventInterface {

    private LocationManager mgrGps; // GPS
    private LocationManager mgrNet; // net location
    private TelephonyManager telephonyManager; // get net / cellid / rss

    // CPU will not sleep while this application is running
    private PowerManager pm;
    private WakeLock wakeLock;

    private Timer myTimer; // this will trigger logging / estimation

    // Estimation algorithm
    private DbSequences db;
    private MatchingAlg malg;
    private ExtractionAlg extalg;
    private EstimationAlg estalg;
    private RunCellidSeq run_seq;
    private CurrentSequence cseq;
    private VirtualCellid vcid;
    private PrevLogPoint prevlog;

    private long logInterval = 1000; // millisecond
    private long pollInterval = 1000; // millisecond
    private long timeoutInterval = 20000; // millisecond
    private long firstWaitInterval = 5000; // millisecond

    private final String logfilename = "cellidtogpslog.txt";
    // private final String dbfilename = "cellidtogpsin.txt";
    private final String dbfilename = "cellidtogpsdb.txt";
    private final String dboutfilename = "cellidtogpsdbout.txt";
    private FileLogger logger;

    private static TextView output;
    private TextViewLogger view;

    private boolean GPS_ON;
    private int turnOffCounter = Const.TURN_OFF_COUNT;
    private int gpsOnCnt = 0, gpsOffCnt = 0;

    private int lastCellid = 0; // last cell id
    private int lastStrength;   // last signal strength
    private int lastLac;        // last location code

    private double gpsLat, gpsLng, gpsAcc; // last GPS location
    private double netLat, netLng, netAcc; // last network location
    private long lastGpsTime = 0;

    private Point currEst = null;
    private double dist_gps_vs_est = 0;
    private double dist_net_vs_gps = 0;
    private double dist_est_vs_net = 0;
    private double dist_gps_vs_alg = 0;
    private int est_cnt = 0;
    private double sum_est_err = 0;
    Vector<Double> err_array = new Vector<Double>();

    private long prevLogTime = 0;       // last log time (ms)
    private long prevNullTime = 0;      // last null log time (ms)


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        output = (TextView) findViewById(R.id.outputText);
        logger = new FileLogger(logfilename);
        view = new TextViewLogger(output);

        mgrGps = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mgrNet = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
        wakeLock.acquire();

        db = new DbSequences();
        cseq = new CurrentSequence();
        malg = new MatchingAlg(db);
        extalg = new ExtractionAlg(malg, db, cseq);
        estalg = new EstimationAlg(malg, db);
        run_seq = new RunCellidSeq();
        vcid = new VirtualCellid();
        prevlog = new PrevLogPoint();

        setup_log_interface();

        GPS_ON = true;

        // /////////////////////////////////////////////////////////

        printBoth("\n#==========================\n# starting...");
        printwtln("# STARTING");

        try {
            if (Prefs.getLoadDb(this) == true) {
                File root = Environment.getExternalStorageDirectory();
                File dbfile = new File(root, dbfilename);
                printBoth("\n#using dbfile:" + dbfile.toString());
                // db.constructDbFromLogFile(dbfile.toString(), malg, vcid);
                db.constructDbFromDbFile(dbfile.toString(), vcid);
                prevlog.resetAll();
            }
            printBoth("\n# GOING INTO ESTIMATION IN " + firstWaitInterval/1000 + " SEC\n");
        } catch (Exception e) {
            view.println("exception happend");
        }

        mgrGps.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                pollInterval, 0, gpsLocationListener);
        mgrNet.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                pollInterval, 1, netLocationListener);
        telephonyManager.listen(phoneStateListener,
                PhoneStateListener.LISTEN_SIGNAL_STRENGTH);

        myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                TimerMethod();
                }
                }, firstWaitInterval, logInterval);
    }

    private void TimerMethod() {
        // This method is called directly by the timer
        // and runs in the same thread as the timer.

        // We call the method that will work with the UI
        // through the runOnUiThread method.
        this.runOnUiThread(Timer_Tick);
    }

    private Runnable Timer_Tick = new Runnable() {
        public void run() {
            // This method runs in the same thread as the UI.
            // Do something to the UI thread here
            getMyLocation();
        }
    };

    private void getMyLocation() {
        String latLngString = "No location found";
        String cellInfoString = "No net info found";
        String estString = "No estimation found";
        String resultString = "No result yet", resultString2 = "";

        // get system time
        long stime = System.currentTimeMillis();

        int cid = lastCellid;
        int lac = lastLac;
        double lat = gpsLat;
        double lng = gpsLng;
        double acc = gpsAcc;

        // clear screen
        view.clear();

        // ///////////////

        if ((lastCellid != 0) && (gpsLat != 0.0) && (netLat != 0.0)) {
	        if ((stime - lastGpsTime < timeoutInterval) && (acc < Const.ACC_THRESH)) {
	            // we do it only when we can validate.. (for exp)
	            estimatePosition(cid, lat, lng, stime / 1000);
	        } else {
	            currEst = null;
	        }
        } else {
        	currEst = null;
        }

        // ///////////////

        if ((stime - lastGpsTime < timeoutInterval) && (gpsLat != 0.0))
            latLngString = String.format("[GPS]\n Lat: %.6f\n Lng: %.6f\n Acc: %.1f\n Time: %d", gpsLat, gpsLng, gpsAcc, stime/1000);

        cellInfoString = String.format("[CID]\n CellId: %d\n LocationCode: %d\n SignalStrength: %d", cid, lac, lastStrength);

        if (currEst != null)
            estString = String.format("[EST]\n Lat: %.6f \n Lng: %.6f\n Err: %.1f (vs. %.1f)",
                    currEst.x, currEst.y, dist_gps_vs_est, dist_net_vs_gps);

        resultString = String.format("[RESULT1] cnt %d, succcnt %d, failcnt %d, avgerr %.1f, maxerr %.1f, medianerr %.1f",
                estalg.succ_cnt + estalg.fail_cnt, estalg.succ_cnt,
                estalg.fail_cnt, estalg.average_error(),
                estalg.max_error(), estalg.median_error());
        resultString2 = String.format("[RESULT2] estcnt %d, avgerr %.1f, maxerr %.1f, medianerr %.1f, gpsOn %d, gpsOff %d, ratio %.1f%%",
                est_cnt, this.average_error(), this.max_error(),
                this.median_error(), gpsOnCnt, gpsOffCnt,
                (double) gpsOnCnt * 100.0 / (double) (gpsOnCnt + gpsOffCnt));

        view.printwt("\n");
        view.println("Your Current Position is:\n" + latLngString);
        view.println(estString);
        view.println(cellInfoString);
        view.println(resultString);
        view.println(resultString2);

        if (GPS_ON == true)
            view.println("[GPS_ON]");
        else
            view.println("[GPS_OFF]");
    }

    private void updateWithNewGpsLocation(Location location) {
        // get system time
        long stime = System.currentTimeMillis();

        // get the cell information
        int phone_type = telephonyManager.getPhoneType();
        int cid = -1; 
        int lac = -1;
        if (phone_type == TelephonyManager.PHONE_TYPE_GSM) {
        	GsmCellLocation gsmLocation = (GsmCellLocation) telephonyManager.getCellLocation();
        	cid = gsmLocation.getCid() & 0xffff;
        	lac = gsmLocation.getLac() & 0xffff;
        } else if (phone_type == TelephonyManager.PHONE_TYPE_CDMA) {
        	CdmaCellLocation cdmaLocation = (CdmaCellLocation) telephonyManager.getCellLocation();
        	cid = cdmaLocation.getBaseStationId();
        	lac = cdmaLocation.getNetworkId();
        } else {
        	printwtln(String.format("## ERROR !!"));
        }
        if (cid != lastCellid) {
            printwtln(String.format("## [CID] CellId: %d LocationCode: %d SignalStrength: %d", cid, lac, lastStrength));
            lastCellid = cid;
            lastLac = lac;
        }

        // see location
        if (location != null) {
            double lat = location.getLatitude();
            double lng = location.getLongitude();
            double acc = location.getAccuracy();
            gpsLat = lat;
            gpsLng = lng;
            gpsAcc = acc;
            lastGpsTime = System.currentTimeMillis();

            // log to a file
            String logString = String.format("%.6f %.6f %d %.1f %d %d %d %.6f %.6f %.1f RAWLOG",
                    lat, lng, stime / 1000, acc, cid, lac, lastStrength, netLat, netLng, netAcc);
            printwtln(logString);
            prevLogTime = stime;
            prevNullTime = stime;
        } else {
            // log null to a file
            if ((stime - prevLogTime >= timeoutInterval)
                    && (stime - prevNullTime >= timeoutInterval)) {
                String logString = String.format("%.6f %.6f %d %.1f %d %d %d %.6f %.6f %.1f RAWLOG",
                        0.0, 0.0, stime / 1000, 0.0, cid, lac, lastStrength, netLat, netLng, netAcc);
                printwtln(logString);
                prevNullTime = stime;
            }
        }
    }

    private void updateWithNewCellid(Location location) {
        // see location
        if (location != null) {
            double lat = location.getLatitude();
            double lng = location.getLongitude();
            double acc = location.getAccuracy();			
            if (netLat != lat || netLng != lng || netAcc != acc) {
                printwtln(String.format("## [NET] Lat: %.6f Lng: %.6f Acc: %.1f", lat, lng, acc));
            }
            netLat = lat;
            netLng = lng;
            netAcc = acc;			
        }
    }

    private final LocationListener netLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            updateWithNewCellid(location);
        }
        public void onProviderDisabled(String provider) {
        }
        public void onProviderEnabled(String provider) {
        }
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    private final LocationListener gpsLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            updateWithNewGpsLocation(location);
        }
        public void onProviderDisabled(String provider) {
        }
        public void onProviderEnabled(String provider) {
        }
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    private final PhoneStateListener phoneStateListener = new PhoneStateListener() {
        public void onCallForwardingIndicatorChanged(boolean cfi) {
        }

        public void onCallStateChanged(int state, String incomingNumber) {
        }

        public void onCellLocationChanged(CellLocation location) {
        }

        public void onDataActivity(int direction) {
        }

        public void onDataConnectionStateChanged(int state) {
        }

        public void onMessageWaitingIndicatorChanged(boolean mwi) {
        }

        public void onServiceStateChanged(ServiceState serviceState) {
        }

        // public void onSignalStrengthsChanged(SignalStrength signalStrength){
        public void onSignalStrengthChanged(int signalStrength) {
            lastStrength = signalStrength;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(this, Prefs.class));
                return true;
            case R.id.exit:
                printBoth("Exiting....");
                printwtln(String.format("## [RESULT1] cnt %d, succcnt %d, failcnt %d, avgerr %.1f, maxerr %.1f, medianerr %.1f",
                            estalg.succ_cnt + estalg.fail_cnt, estalg.succ_cnt,
                            estalg.fail_cnt, estalg.average_error(),
                            estalg.max_error(), estalg.median_error()));
                printwtln(String.format("## [RESULT2] estcnt %d, avgerr %.1f, maxerr %.1f, medianerr %.1f, gpsOn %d, gpsOff %d, ratio %.1f%%",
                            est_cnt, this.average_error(), this.max_error(),
                            this.median_error(), gpsOnCnt, gpsOffCnt,
                            (double) gpsOnCnt * 100.0
                            / (double) (gpsOnCnt + gpsOffCnt)));

                myTimer.cancel();
                mgrGps.removeUpdates(gpsLocationListener);
                mgrNet.removeUpdates(netLocationListener);
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
                wakeLock.release();

                if (Prefs.getSaveDb(this) == true) {
                    File root = Environment.getExternalStorageDirectory();
                    File dboutfile = new File(root, dboutfilename);
                    db.printDbToDbFile(dboutfile.toString(), vcid);
                }

                finish();
                return true;
        }
        return false;
    }

    /*
     * time must be 'seconds'
     */
    public void estimatePosition(int cid, double lat, double lon, long time) {

        int cellid = vcid.get(cid);

        prevlog.detect_change_from_prev_log(cellid, lat, lon, time);

        boolean new_seq_detected = prevlog.new_seq_detected;
        boolean new_diff_cell_detected = prevlog.new_diff_cell_detected;
        boolean new_same_cell_detected = prevlog.new_same_cell_detected;
        boolean old_cell_detected = prevlog.old_cell_detected;

        if ((GPS_ON == true) && (lat != 0.0)) {
            
            // push current location:
            if (prevlog.jump_time >= Const.SEC) {
                //if (!cellid_change_detected)
                    cseq.addPointToCurrentCellidPoint(lat, lon, time);
            }

            // let's select an algorithm for sequence extraction from current sequence.
            if (new_seq_detected == true) {
                extalg.move_curr_sequence_to_db();

                // re-add the new values into the current sequence to start a new sequence
                cseq.addCellidPoint(cellid, lat, lon, time);
            }
            else if (new_diff_cell_detected == true) {
                // cell-id change has been detected
                extalg.cut_curr_sequence_to_db(cellid);

                // add the new values into the current sequence to start a new sequence
                cseq.addCellidPoint(cellid, lat, lon, time);
            }
            else if (new_same_cell_detected == true) {

                // add the same cellid into the current sequence
                cseq.addCellidPoint(cellid, lat, lon, time);
            }
        }

        //
        // Assume that I only know 'cellid' and 'time' from now on !!!!!
        //
        boolean run_has_changed = false;

        if (new_seq_detected == true) {
            run_seq.clear();            // empty run current sequence

            // re-add the new values into the current sequence to start a new sequence
            run_seq.insert_to_run_sequence(cellid, time);
            run_has_changed = true;
        }
        else if (new_diff_cell_detected == true) {
            // add the new values into the current sequence
            run_seq.insert_to_run_sequence(cellid, time);
            run_has_changed = true;
        }
        else if (new_same_cell_detected == true) {
            // add the new values into the current sequence
            run_seq.insert_to_run_sequence(cellid, time);
            run_has_changed = true;
        }

        // check and rate previous sequence selection
        if ((new_diff_cell_detected == true) || (new_same_cell_detected == true)) {
            if (estalg.cellid_change_as_expected(cellid) != true) {
                //if (estalg.curr_match_len == 1)
                //    turn_on_gps();
            }
        }

        long time_in_cellid = prevlog.time_passed_in_cellid(time);

        dist_gps_vs_est = -1;
        dist_net_vs_gps = 0;
        dist_est_vs_net = -1;
        dist_gps_vs_alg = 0;

        // estimate current location!!!!!!!!!!!!!!!!!!!!!!!!
        currEst = estalg.estimate_position(run_seq, run_has_changed, time_in_cellid, time);

        // calculate errors
        dist_net_vs_gps = Point.distance(lat, lon, netLat, netLng);
        if (currEst != null) {
            dist_gps_vs_est = Point.distance(lat, lon, currEst.x, currEst.y);
            dist_est_vs_net = Point.distance(currEst.x, currEst.y, netLat, netLng);
        }
        if (GPS_ON == true) {
            dist_gps_vs_alg = 0.0;
        } else {
            dist_gps_vs_alg = dist_gps_vs_est;
        }

        if (currEst != null) {
            //
            // estimation actually happened !!
            //
            this.add_error(dist_gps_vs_alg);
            estalg.add_error(dist_gps_vs_est);
            println(String.format("PREDICTION:\t%.6f\t%.6f (type %d cnt %d)",
                        currEst.x, currEst.y, currEst.flag, estalg.succ_cnt));
            println(String.format("    ACTUAL:\t%.6f\t%.6f ( error %.1f )", lat, lon, dist_gps_vs_est));

            if (GPS_ON == true) {
                if (dist_gps_vs_est < Const.TURN_OFF_ERR_THRESH) {
                    if (--turnOffCounter <= 0) {
                        if (turn_off_gps(time) == true) {
                            extalg.move_curr_sequence_to_db();
                        }
                    }
                } else {
                    turnOffCounter = Const.TURN_OFF_COUNT;
                }
            }
        } else {
            turn_on_gps();
            println(String.format("    ACTUAL:\t%.6f\t%.6f (estimate failed)", lat, lon));
            estalg.increment_fail_cnt();
        }

        if (GPS_ON == true) {
            gpsOnCnt++;
        } else {
            gpsOffCnt++;
        }

        // log the position estimate of our algorithm
        if (GPS_ON == true) {
            if (currEst != null) {
                println(String.format("ESTIMATE %.6f %.6f %d %.6f %.6f %.1f %.1f %.6f %.6f %.1f %.1f %.1f %d GPS_ON", 
                            currEst.x, currEst.y, time, 
                            lat, lon, dist_gps_vs_alg, dist_gps_vs_est, 
                            netLat, netLng, dist_net_vs_gps, dist_est_vs_net, netAcc, cellid));
            } else {
                println(String.format("ESTIMATE %.6f %.6f %d %.6f %.6f %.1f %.1f %.6f %.6f %.1f %.1f %.1f %d GPS_ON", 
                            -1.0, -1.0, time, 
                            lat, lon, dist_gps_vs_alg, dist_gps_vs_est, 
                            netLat, netLng, dist_net_vs_gps, dist_est_vs_net, netAcc, cellid));
            }
        } else {
            if (currEst != null) {
                println(String.format("ESTIMATE %.6f %.6f %d %.6f %.6f %.1f %.1f %.6f %.6f %.1f %.1f %.1f %d GPS_OFF", 
                            currEst.x, currEst.y, time, 
                            lat, lon, dist_gps_vs_alg, dist_gps_vs_est, 
                            netLat, netLng, dist_net_vs_gps, dist_est_vs_net, netAcc, cellid));
            } else {
                println(String.format("ESTIMATE ERROR?"));
            }
        }

        // update previous info and go to next
        if (GPS_ON == true)
            prevlog.set(cellid, lat, lon, time);
        else if (currEst != null)
            prevlog.set(cellid, currEst.x, currEst.y, time);

        if ((new_seq_detected == true) || (new_diff_cell_detected == true) || (new_same_cell_detected == true)) {
            if (GPS_ON == true)
                prevlog.setFirstInCellid(lat, lon, time);
            else if (currEst != null)
                prevlog.setFirstInCellid(currEst.x, currEst.y, time);
        }

        // if GPS is OFF, turn on GPS if we have not in last 10 minutes
        if (GPS_ON == false) {
            //if ((time - prevGpsOffTime > 20 * 60 ) && (time_in_cellid > 10 * 60))
            //	turn_on_gps();
        }

    }

    public void turn_on_gps() {
        if (GPS_ON == false) {
            println("turn on gps");
            GPS_ON = true;
            turnOffCounter = Const.TURN_OFF_COUNT;
        }
    }

    public boolean turn_off_gps(long time) {
        if (GPS_ON == true) {
            println("turn off gps");
            GPS_ON = false;
            return true;
        }
        return false;
    }

    public void println(String str) { logger.println(str); }
    public void print(String str) {	logger.print(str); }
    public void printwtln(String str) {	logger.printlnwt(str); }
    public void printBoth(String str) {
        view.println(str);
        logger.println(str);
    }

    public void setup_log_interface() {
        CellidPoint.logInt = this;
        CurrentSequence.logInt = this;
        DbSequences.logInt = this;
        EstimationAlg.logInt = this;
        ExtractionAlg.logInt = this;
        HourOfDay.logInt = this;
        MatchingAlg.logInt = this;
        PrevLogPoint.logInt = this;
        RunCellidSeq.logInt = this;
        Sequence.logInt = this;
    }

    public void add_error(double err) {
        est_cnt += 1;
        sum_est_err += err;
        err_array.add(err);
    }

    public double median_error() {
        if (err_array.size() > 0) {
            Collections.sort(err_array);
            int idx = err_array.size() / 2;
            return (double) err_array.get(idx);
        }
        return 0.0;
    }

    public double average_error() {
        // calculate average estimation error
        if (est_cnt > 0)
            return (sum_est_err * 1000.0 / (double) est_cnt) / 1000.0;
        return 0.0;
    }

    public double max_error() {
        if (err_array.size() > 0)
            return (double) Collections.max(err_array);
        return 0.0;
    }
}
