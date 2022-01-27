package com.example.roadconditions;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;

import com.example.accelerometer.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;





import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import au.com.bytecode.opencsv.CSVWriter;

@SuppressLint("NewApi")
public class MainActivity extends Activity implements OnClickListener, SensorEventListener{
	private SensorManager sensorManager;
	private Button btnStart, btnStop, btnData;
	LocationManager lmgr;
	GoogleMap map;
	private MapFragment mapFragment;
	Location currentLocation;
	Location prevLocation = null;
	double distanceRan = 0;
	double totalDistance;
	double avQuality = 0;
	int sumQuality = 0;
	private TextView text1;
	private TextView text2;
	private TextView text3;
	private boolean started = false;
	float accel[] = new float[3];
	String timeSeconds;
	long count = 0;
	long bump = 0;
	double sum = 0;
	double mean = 0;
	double stdDev = 0;
	double speed = 0;
	long time = 0;
	long start = 0;
	long tooSmall = 0;
	float maxRange = 0;
	double min = 0;
	double max = 0;
	int record = 1;
	int fileCount;
	int condition;
	boolean foundMax = false;
	boolean foundMin = false;
	TimerTask wait;
	TimerTask task;
	boolean taskStarted = false;
	boolean calcDev = false;
	double yvalue[] = new double[100];
	int yindex = 0;
	CountDownTimer countdown;
	CSVWriter csvWriteOutput;
	CSVWriter csvWriteData;
	
	public static final int LOW = 0;
	public static final int AVERAGE = 1;
	public static final int HIGH = 2;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        text1 = (TextView) findViewById(R.id.textView1);
        //text2 = (TextView) findViewById(R.id.textView2);
        //text3 = (TextView) findViewById(R.id.textView3);
        btnStart = (Button) findViewById(R.id.btnStart);
        btnStop = (Button) findViewById(R.id.btnStop);
        btnData = (Button) findViewById(R.id.btnData);
        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);
        btnData.setOnClickListener(this);
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        btnData.setEnabled(false);
        btnStart.setAlpha(1);
        btnStop.setAlpha((float) 0.5);
        btnData.setAlpha((float) 0.5);
        mapFragment = ((MapFragment) getFragmentManager().findFragmentById(R.id.map));
        mapFragment.getView().setVisibility(View.GONE);
		map = mapFragment.getMap();
		map.setMyLocationEnabled(true);
        lmgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		lmgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5,
				locationListener);
		currentLocation = lmgr.getLastKnownLocation(lmgr.GPS_PROVIDER);
        SharedPreferences app_preferences = 
                PreferenceManager.getDefaultSharedPreferences(this);
            // Get the value for the run counter
            fileCount = app_preferences.getInt("fileCount", 0);
            totalDistance = app_preferences.getFloat("totalDistance", 0);
           /* Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int height = size.y;
            text1.setHeight(height - 100);*/
        text1.setText("\n\n\n\t\t\t\t\t\tPress Start to begin.");
        
	}
	
	@Override
    protected void onResume() {
        super.onResume();
        lmgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5,
				locationListener);
    }
	
	@Override
    protected void onDestroy() {
        super.onResume();
        lmgr.removeUpdates(locationListener);
        sensorManager.unregisterListener(accelistener);
    }
 
    @Override
    protected void onPause() {
        super.onPause();
            SharedPreferences app_preferences = 
                    PreferenceManager.getDefaultSharedPreferences(this);

             SharedPreferences.Editor editor = app_preferences.edit();
                editor.putInt("fileCount", fileCount);
                editor.putFloat("totalDistance", (float) totalDistance);
                editor.commit(); // Very important
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
        case R.id.btnStart:
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
            btnData.setEnabled(false);
            btnStart.setAlpha((float) 0.5);
            btnStop.setAlpha(1);
            btnData.setAlpha((float) 0.5);
            // save prev data if available
            mapFragment.getView().setVisibility(View.GONE);
            map.clear();
            countdown = new CountDownTimer(16000, 1000) {

                public void onTick(long millisUntilFinished) {
                    text1.setText("\n\n\n\t\t\t\t\t\tSeconds left: " + millisUntilFinished / 1000);
                }

                public void onFinish() {
                    text1.setText("\n\n\n\t\t\t\t\t\tData Analysis in progress.");
                }
             }.start();
            new Timer().schedule(wait = new TimerTask() {
                @Override
                public void run() {
                    //This code is run all seconds
		            File exportDir = new File(Environment.getExternalStorageDirectory(), "");
		            if (!exportDir.exists()) {
		            exportDir.mkdirs();
		            }
		            File file = new File(exportDir, "SensorData" + fileCount + ".csv"); // accelCSV is the name of my file
		            try {
		            file.createNewFile();
		            csvWriteOutput = new CSVWriter(new FileWriter(file));
		            } 
		            catch (IOException e) { //you  need this
		            String LOGTAG = "Accelerometer";
		    		Log.i(LOGTAG, "SQL error"); //change this log msg --> LOGTAG is string - the name of my app
		            }
		            File file2 = new File(exportDir, "Results" + fileCount + ".csv"); // accelCSV is the name of my file
		            try {
		            file2.createNewFile();
		            csvWriteData = new CSVWriter(new FileWriter(file2));
		            } 
		            catch (IOException e) { //you  need this
		            String LOGTAG = "Accelerometer";
		    		Log.i(LOGTAG, "SQL error"); //change this log msg --> LOGTAG is string - the name of my app
		            }
		            String[] csvData = { "Time (s)", 
		        			"Mean (m/s^2)", "Standard Deviation (m/s^2)", "Confidence", 
		        			"Condition: (Low = 0, Average = 1, High = 2)", "Anomaly Count", "Distance this run (km)"};
		        	csvWriteData.writeNext(csvData);
		        	started = true;
                	start = System.nanoTime();
                    Sensor accel = sensorManager
                            .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                    maxRange = accel.getMaximumRange();
                    sensorManager.registerListener(accelistener, accel,
                            SensorManager.SENSOR_DELAY_FASTEST);
                }
            }, 16000);
            
            break;
        case R.id.btnStop:
        	wait.cancel();
        	if(started == true)
        		task.cancel();
        	countdown.cancel();
            btnStart.setEnabled(true);
            btnStop.setEnabled(false);
            btnData.setEnabled(true);
            btnStart.setAlpha(1);
            btnStop.setAlpha((float) 0.5);
            btnData.setAlpha(1);
            started = false;
            BigDecimal dist = BigDecimal.valueOf(distanceRan).setScale(3, BigDecimal.ROUND_HALF_UP);
            BigDecimal totalDist = BigDecimal.valueOf(totalDistance).setScale(3, BigDecimal.ROUND_HALF_UP);
            BigDecimal sp = BigDecimal.valueOf(speed).setScale(3, BigDecimal.ROUND_HALF_UP);
            if((int) avQuality == 0)
            {
            text1.setText("\n\n\n\t\t\t\t\t\tStats for this jog:\n\n\t\t\t\t\t\t" + "Mean Road condition: " + "LOW" + "\n\t\t\t\t\t\t"
            		+ "Distance traveled: " + dist + "(km)" + "\n\t\t\t\t\t\t" + "Total distance: " + totalDist + "(km)" +"\n\t\t\t\t\t\t"
            		+ "Average speed: " + sp + "(m/s)" + "\n\t\t\t\t\t\t"
            		+ "Time taken: " + (time / 1000) + "." + (time % 1000) + "(s)" + "\n\t\t\t\t\t\t");
            }
            else if((int) avQuality == 1)
            {
                text1.setText("\n\n\n\t\t\t\t\t\tStats for this jog:\n\n\t\t\t\t\t\t" + "Mean Road condition: " + "AVERAGE" + "\n\t\t\t\t\t\t"
                		+ "Distance traveled: " + dist + "(km)" + "\n\t\t\t\t\t\t" + "Total distance: " + totalDist + "(km)" +"\n\t\t\t\t\t\t"
                		+ "Average speed: " + sp + "(m/s)" + "\n\t\t\t\t\t\t"
                		+ "Time taken: " + (time / 1000) + "." + (time % 1000) + "(s)" + "\n\t\t\t\t\t\t");
            }
            else
            {
                text1.setText("\n\n\n\t\t\t\t\t\tStats for this jog:\n\n\t\t\t\t\t\t" + "Mean Road condition: " + "HIGH" + "\n\t\t\t\t\t\t"
                		+ "Distance traveled: " + dist + "(km)" + "\n\t\t\t\t\t\t" + "Total distance: " + totalDist + "(km)" +"\n\t\t\t\t\t\t"
                		+ "Average speed: " + sp + "(m/s)" + "\n\t\t\t\t\t\t"
                		+ "Time taken: " + (time / 1000) + "." + (time % 1000) + "(s)" + "\n\t\t\t\t\t\t");
            }
            time = 0;
            mean = 0;
            sum = 0;
            count = 0;
            stdDev = 0;
            bump = 0;
            tooSmall = 0;
            max = 0;
            min = 0;
            sensorManager.unregisterListener(accelistener);
            lmgr.removeUpdates(locationListener);
            fileCount++;
            break;
        case R.id.btnData:
        	//wait.cancel();
        	task.cancel();
            btnStart.setEnabled(true);
            btnStop.setEnabled(false);
            btnStart.setAlpha(1);
            btnStop.setAlpha((float) 0.5);
            mapFragment.getView().setVisibility(View.VISIBLE);
            started = false;
            time = 0;
            mean = 0;
            sum = 0;
            count = 0;
            stdDev = 0;
            bump = 0;
            tooSmall = 0;
            max = 0;
            min = 0;
            try {
				csvWriteOutput.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            try {
				csvWriteData.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
           
            break;
        default:
            break;
        }
	}
	
	public long getTime()
	{
		return time;
	}
	
	SensorEventListener accelistener = new SensorEventListener(){

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		if (started) {
            double x = event.values[0];
            double y = event.values[1];
            double z = event.values[2];
            long end = System.nanoTime();
            long elapsedTime = end - start;
            count++;
            time = TimeUnit.SECONDS.toSeconds(elapsedTime) / 1000000;
          analyzeData(x, y, z, time);
          
        }
	}
	
	};

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	protected void analyzeData(double x, double y, double z, long time) {
		// TODO Auto-generated method stub
		 
         //SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
         //String newtime = df.format(time);
         String newtime = (time / 1000) + "." + (time % 1000);
         text1.setText("\n\n\n\t\t\t\t\t\t" + newtime);
		//ramp-speed - play with this value until satisfied
        float kFilteringFactor = 0.1f;

        //acceleration.x,.y,.z is the input from the sensor

        //result.x,.y,.z is the filtered result

        //high-pass filter to eliminate gravity
        accel[0] = (float) (x * kFilteringFactor + accel[0] * (1.0f - kFilteringFactor));
        accel[1] = (float) (y * kFilteringFactor + accel[1] * (1.0f - kFilteringFactor));
        accel[2] = (float) (z * kFilteringFactor + accel[2] * (1.0f - kFilteringFactor));
        double resultx = x - accel[0];
        double resulty = y - accel[1];
        double resultz = z - accel[2];
        String[] values = { String.valueOf((time / 1000) + "." + (time % 1000)), String.valueOf(resultx), String.valueOf(resulty), String.valueOf(resultz) };
        csvWriteOutput.writeNext(values);
        if(calcDev == false && yindex < yvalue.length)
        {
        	yvalue[yindex] = resulty;
        	yindex++;
        }
        sum = sum + resulty;
        mean = sum / count;
        if(calcDev == false && yindex == yvalue.length) //calculate the standard deviation
        {
        	calcDev = true;
        	double sum1 = 0;
        	double division = 0;
        	
	        for (int i = 0; i < yvalue.length; i++)
	        {
	        	sum1 = sum1 + Math.pow(yvalue[i] - mean, 2);
	        }
	        division = sum1 / yvalue.length-1;
	        stdDev = Math.sqrt(division);
			      
        }
        if((time / 1000) % 10 == 0)
        {
        	yindex = 0;
        	calcDev = false;
        }
        if(calcDev == true && (time / 1000) >= 10 && !Double.isNaN(stdDev)) //check for bumps in the road
        {
	        if((resulty < mean - (2.28 * stdDev)) || (resulty > mean + (2.28 * stdDev)))
	        	bump++;
        }
        if(resulty > max)
        {
        	max = resulty;
        	
        }
        if(resulty < min)
        {
        	min = resulty;
        }
        if(taskStarted == false) //start analyzing the data for the past 10 seconds
        {
        	taskStarted = true;
      new Timer().schedule(task = new TimerTask() {
            @Override
            public void run() {
                //This code is run all seconds
            	if(bump < 2)
            	{
            		condition = HIGH;
            		sumQuality = sumQuality + 2;
            	}
            	else if(bump < 3)
            	{
            		condition = AVERAGE;
            		sumQuality = sumQuality + 1;
            	}
            	else
            	{
            		condition = LOW;
            		sumQuality = sumQuality + 0;
            	}
            	writeData();
            	
            	bump = 0;
            	
            	runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    	addCircle(condition);
                    	runDistance();
                    	speed = (distanceRan * 1000)/ (getTime() / 1000);
                    	record++;
                    }
                });
            	avQuality = sumQuality / record;
            }
        }, 20000, 10000);
        }
        //text1.setText("Max range: " + maxRange + "\n\t\t\tMean: " + mean + "\n\t\t\tDeviation: " + stdDev + "\n\t\t\tTime: " + newtime + "\t" + time + "\n\t\t\tAccelerometer: \n\t\t\t\t X = " + resultx + " m/s^2 \n\t\t\t\t Y = " + resulty + " m/s^2 \n\t\t\t\t Z = " + resultz + " m/s^2");
       // text2.setText("" + bump +"\n" + "Max: " + max + "\n" + "Min: " + min + "\n" + "Distance: " + distanceRan + "\n" + "Average speed: " + speed);
        
	}
	
	protected void runDistance() {
		if(prevLocation != null)
		{
		distanceRan = distanceRan + calcDistance(prevLocation.getLatitude(), prevLocation.getLongitude(), currentLocation.getLatitude(), currentLocation.getLongitude());
		totalDistance = totalDistance + distanceRan;
		}
	}

	protected void addCircle(int condition) {
		// TODO Auto-generated method stub
		// Instantiates a new CircleOptions object and defines the center and radius
		CircleOptions circleOptions;
		if(condition == 0)
		{
			circleOptions = new CircleOptions()
    	    .center(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()))
    	    .radius(8)
    	    .strokeColor(Color.RED)
    	    .fillColor(Color.RED); // In meters
		}
		else if(condition == 1)
		{
			circleOptions = new CircleOptions()
    	    .center(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()))
    	    .radius(8)
    	    .strokeColor(Color.YELLOW)
    	    .fillColor(Color.YELLOW); // In meters
		}
		else
		{
			circleOptions = new CircleOptions()
    	    .center(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()))
    	    .radius(8)
    	    .strokeColor(Color.GREEN)
    	    .fillColor(Color.GREEN); // In meters
		}
		map.addCircle(circleOptions);
		
	}

	protected void writeData() {
		// TODO Auto-generated method stub
		if(condition == 0)
		{
		String[] csvData = { ((getTime() / 1000) + "." + (getTime() % 1000)), 
    			String.valueOf(mean), String.valueOf(stdDev), "97.7%", 
    			"LOW", String.valueOf(bump), String.valueOf(distanceRan)};
    	csvWriteData.writeNext(csvData);
		}
		else if(condition == 1)
		{
			String[] csvData = { ((getTime() / 1000) + "." + (getTime() % 1000)), 
	    			String.valueOf(mean), String.valueOf(stdDev), "97.7%", 
	    			"AVERAGE", String.valueOf(bump), String.valueOf(distanceRan)};
	    	csvWriteData.writeNext(csvData);
		}
		else
		{
			String[] csvData = { ((getTime() / 1000) + "." + (getTime() % 1000)), 
	    			String.valueOf(mean), String.valueOf(stdDev), "97.7%", 
	    			"HIGH", String.valueOf(bump), String.valueOf(distanceRan)};
	    	csvWriteData.writeNext(csvData);
		}
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		
	}
	
	LocationListener locationListener = new LocationListener() {

		@Override
		public void onLocationChanged(Location location) {
			// TODO Auto-generated method stub
			if (location != null) {
				prevLocation = currentLocation;
				currentLocation = location;
				
					
			}
		}

		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub

		}
	};
	
	public double calcDistance(double lat1, double lon1, double lat2, double lon2)
	{
			double R = 6372.8; // In kilometers
	        double dLat = Math.toRadians(lat2 - lat1);
	        double dLon = Math.toRadians(lon2 - lon1);
	        lat1 = Math.toRadians(lat1);
	        lat2 = Math.toRadians(lat2);
	 
	        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
	        double c = 2 * Math.asin(Math.sqrt(a));
	        return R * c;
	    }

}
