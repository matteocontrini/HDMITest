package io.ourglass.hdmitest.Views;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import io.ourglass.hdmitest.RtkHdmiWrapper;
import io.ourglass.hdmitest.R;

public class HDMIView2 extends RelativeLayout {

    private static final String TAG = "HDMIView2";

    private Context mContext;

    private LayoutInflater mInflater;

    public RtkHdmiWrapper rtkHdmiWrapper;
    private boolean hdmiConnectedState = false;

    RelativeLayout mHdmiHolder;
    TextView mHdmiErrorTextView, mDebugStateTV, mDebugErrorTV, mDebugMsgTV;

    HDMIViewListener mListener;

    public boolean hdmiSurfaceReady = false;
    public boolean hdmiDriverReady = false;
    public boolean hdmiPHYConnected = false;

    public boolean enableAutostart = true;
    public boolean mDebugMode = false;

    public boolean hasIssuedPlayToDriver = false; // issuing 2 causes lockup right now!

    public interface HDMIViewListener {
        public void ready();
        public void error(RtkHdmiWrapper.OGHdmiError error);
    }

    private RtkHdmiWrapper.RtkWrapperListener mRtkWrapperListener = new RtkHdmiWrapper.RtkWrapperListener() {
        @Override
        public void error(RtkHdmiWrapper.OGHdmiError error, String msg) {

            Log.e(TAG, "HDMI error: " + error.name());
            Log.e(TAG, "HDMI error msg: " + msg);
            if (mDebugMode) {
                addDebugErrorMessage(error.name()+ "\n" + msg);
            }

            switch (error){

                case HDMI_CANT_OPEN_DRIVER:
                    // this is a fucking, we're done
                    mHdmiHolder.setVisibility(View.INVISIBLE);
                    mHdmiErrorTextView.setText("CAN'T ACQUIRE DRIVER");
                    break;

                case FYI:
                    Log.d(TAG, "FYI error: "+msg);
                    break;

                default:
                    mHdmiErrorTextView.setText(error.name());
            }

            //mHdmiHolder.setVisibility(View.INVISIBLE);
            mHdmiErrorTextView.setVisibility(View.VISIBLE);
        }

        @Override
        public void hdmiStateChange(RtkHdmiWrapper.OGHdmiState state) {

            Log.d(TAG, "HDMI state change: " + state.name());
            if (mDebugMode) {
                addDebugStateMessage(state.name());
            }

            switch (state){

                case HDMI_DRIVER_READY:
                    hdmiDriverReady = true;
                    //startIfReady();
                    break;

                case SURFACE_READY:
                    hdmiSurfaceReady = true;
                    //rtkHdmiWrapper.initHDMIDriver();
                    break;

                case HDMI_PHY_CONNECTED:
                    hdmiPHYConnected = true;
                    startIfReady();

                default:
                    Log.d(TAG, "State ignored");
            }

        }

        @Override
        public void fyi(String msg){
            if (mDebugMode) {
                addDebugMessage(msg + "\n");
            }

        }
    };

    public HDMIView2(Context context) {
        super(context);
        init(context);
    }

    public HDMIView2(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HDMIView2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(21)
    public HDMIView2(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    /**
     * Turn on/off debug overlays
     * @param debugModeOn
     */
    public void setDebugMode(boolean debugModeOn){
        mDebugMode = debugModeOn;
        updateDebugViews();
    }

    public void updateDebugViews(){
        int vstate = mDebugMode ? View.VISIBLE : View.INVISIBLE;
        mDebugErrorTV.setVisibility(vstate);
        mDebugStateTV.setVisibility(vstate);
        mDebugMsgTV.setVisibility(vstate);
    }

    public void showDebugViews(){
        mDebugErrorTV.setVisibility(View.VISIBLE);
        mDebugStateTV.setVisibility(View.VISIBLE);
        mDebugMsgTV.setVisibility(View.VISIBLE);
    }

    private void addDebugMessageTo(TextView targetView, String msg){
        String current = (String)targetView.getText();
        current += "\n" + msg;
        targetView.setText(current);
    }

    public void addDebugStateMessage(String msg){
        addDebugMessageTo(mDebugStateTV, msg);
    }

    public void addDebugErrorMessage(String msg){
        addDebugMessageTo(mDebugErrorTV, msg);
    }

    public void addDebugMessage(String msg){
        addDebugMessageTo(mDebugMsgTV, msg);
    }


    public void init(Context context) {

        mContext = context;
        mInflater = LayoutInflater.from(context);
        View v = mInflater.inflate(R.layout.hdmi_view, this, true);

        mHdmiHolder = (RelativeLayout) v.findViewById(R.id.home_ac_hdmi_textureView);
        mHdmiErrorTextView = (TextView)v.findViewById(R.id.home_ac_hdmi_nosignal_text_view);

        mDebugErrorTV = (TextView) v.findViewById(R.id.textViewErr);
        mDebugErrorTV.setText("**** ERRORS ****");

        mDebugStateTV = (TextView) v.findViewById(R.id.textViewState);
        mDebugStateTV.setText("**** STATE ****");

        mDebugMsgTV = (TextView) v.findViewById(R.id.textViewMsg);
        mDebugMsgTV.setText("**** MESSAGES ****");

        updateDebugViews();

    }

    public void startIfReady(){

        Log.d(TAG, "Checking if everything is good to go before starting.");
        if (enableAutostart && hdmiDriverReady && hdmiSurfaceReady && hdmiPHYConnected){
            Log.d(TAG, "Everyone is ready, let's start.");
            resume();
        }

    }

    // All this does is instatiate the driver wrapper, which only populates the target surface and
    // fires up the PHY Broadcast rx. Later it will init audio.
    public void start(HDMIViewListener listener){

        mListener = listener;
        rtkHdmiWrapper = new RtkHdmiWrapper(mContext, mHdmiHolder, mRtkWrapperListener, mDebugMode);

    }


    // LOW LEVEL METHODS. MEANT for DEBUG PURPOSES


    public void initRtkDriver(){
        if ( rtkHdmiWrapper != null ) {
            rtkHdmiWrapper.initHDMIDriver();
        } else {
            addDebugErrorMessage("Can't init driver before it exists!");
            Toast.makeText(mContext, "No Wrapper Yet, Homeboy", Toast.LENGTH_SHORT).show();
        }
    }

    public void resume() {

        if (rtkHdmiWrapper != null && !hasIssuedPlayToDriver) {
            hasIssuedPlayToDriver = true;
            rtkHdmiWrapper.play();
            //rtkHdmiWrapper.setSize(true);
        }
    }

    public void pause() {

        if (rtkHdmiWrapper != null) {
            if (rtkHdmiWrapper.isStreaming()) {
                rtkHdmiWrapper.stopStreamer();
            }

            rtkHdmiWrapper.pause();
        }
    }

    public void destroy(){

        if (rtkHdmiWrapper != null) {
            if (rtkHdmiWrapper.isStreaming()) {
                rtkHdmiWrapper.stopStreamer();
            }

            rtkHdmiWrapper.kill();
        }

    }

    public void streamAudio() {
        if (rtkHdmiWrapper.isStreaming()) {
            rtkHdmiWrapper.stopStreamer();
        } else {
            rtkHdmiWrapper.startStreamer();
        }
    }
}
