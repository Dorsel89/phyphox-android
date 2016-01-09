package de.rwth_aachen.phyphox;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.Vector;

//The sensorInput class encapsulates a sensor, maps their name from the phyphox-file format to
//  the android identifiers and handles their output, which is written to the dataBuffers
public class sensorInput implements SensorEventListener {
    public int type; //Sensor type (Android identifier)
    public long period; //Sensor aquisition period in nanoseconds (inverse rate), 0 corresponds to as fast as possible
    public long t0 = 0; //the start time of the measurement. This allows for timestamps relative to the beginning of a measurement
    public dataBuffer dataX; //Data-buffer for x
    public dataBuffer dataY; //Data-buffer for y (3D sensors only)
    public dataBuffer dataZ; //Data-buffer for z (3D sensors only)
    public dataBuffer dataT; //Data-buffer for t
    private SensorManager sensorManager; //Hold the sensor manager

    private long lastReading; //Remember the time of the last reading to fullfill the rate
    private double avgX, avgY, avgZ; //Used for averaging
    private boolean average = false; //Avergae over aquisition period?
    private int aquisitions; //Number of aquisitions for this average

    public class SensorException extends Exception {
        public SensorException(String message) {
            super(message);
        }
    }

    //The constructor needs the sensorManager, the phyphox identifier of the sensor type, the
    //desired aquisition rate, and the four buffers to receive x, y, z and t. The data buffers may
    //be null to be left unused.
    protected sensorInput(SensorManager sensorManager, String type, double rate, boolean average, Vector<dataBuffer> buffers) throws SensorException {
        this.sensorManager = sensorManager; //Store the sensorManager reference

        if (rate <= 0)
            this.period = 0;
        else
            this.period = (long)((1/rate)*1e9); //Period in ns

        this.average = average;

        //Interpret the type string
        switch (type) {
            case "linear_acceleration": this.type = Sensor.TYPE_LINEAR_ACCELERATION;
                break;
            case "light": this.type = Sensor.TYPE_LIGHT;
                break;
            case "gyroscope": this.type = Sensor.TYPE_GYROSCOPE;
                break;
            case "accelerometer": this.type = Sensor.TYPE_ACCELEROMETER;
                break;
            case "magnetic_field": this.type = Sensor.TYPE_MAGNETIC_FIELD;
                break;
            case "pressure": this.type = Sensor.TYPE_PRESSURE;
                break;
            default: throw  new SensorException("Unknown sensor.");
        }

        //Store the buffer references
        buffers.setSize(4);
        this.dataX = buffers.get(0);
        this.dataY = buffers.get(1);
        this.dataZ = buffers.get(2);
        this.dataT = buffers.get(3);
    }

    //Check if the sensor is available without trying to use it.
    public boolean isAvailable() {
        return (sensorManager.getDefaultSensor(type) != null);
    }

    //Get the internationalization string for a sensor type
    public int getDescriptionRes() {
        switch (type) {
            case Sensor.TYPE_LINEAR_ACCELERATION:
                return R.string.sensorLinearAcceleration;
            case Sensor.TYPE_LIGHT: this.type = Sensor.TYPE_LIGHT;
                return R.string.sensorLight;
            case Sensor.TYPE_GYROSCOPE: this.type = Sensor.TYPE_GYROSCOPE;
                return R.string.sensorGyroscope;
            case Sensor.TYPE_ACCELEROMETER: this.type = Sensor.TYPE_ACCELEROMETER;
                return R.string.sensorAccelerometer;
            case Sensor.TYPE_MAGNETIC_FIELD: this.type = Sensor.TYPE_MAGNETIC_FIELD;
                return R.string.sensorMagneticField;
            case Sensor.TYPE_PRESSURE: this.type = Sensor.TYPE_PRESSURE;
                return R.string.sensorPressure;
        }
        return R.string.unknown;
    }

    //Start the data aquisition by registering a listener for this sensor.
    public void start() {
        this.t0 = 0; //Reset t0. This will be set by the first sensor event

        //Reset averaging
        this.lastReading = 0;
        this.avgX = 0.;
        this.avgY = 0.;
        this.avgZ = 0.;
        this.aquisitions = 0;

        this.sensorManager.registerListener(this, sensorManager.getDefaultSensor(type), SensorManager.SENSOR_DELAY_FASTEST);
    }

    //Stop the data aquisition by unregistering the listener for this sensor
    public void stop() {
        this.sensorManager.unregisterListener(this);
    }

    //This event listener is mandatory as this class implements SensorEventListener
    //But phyphox does not need it
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    //This is called when we receive new data from a sensor. Append it to the right buffer
    public void onSensorChanged(SensorEvent event) {
        if (t0 == 0)
            t0 = event.timestamp; //Any event sets the same t0 for all sensors

        //From here only listen to "this" sensor
        if (event.sensor.getType() == type) {
            if (average) {
                //We want averages, so sum up all the data and count the aquisitions
                avgX += event.values[0];
                avgY += event.values[1];
                avgZ += event.values[2];
                aquisitions++;
            } else {
                //No averaging. Just keep the last result
                avgX = event.values[0];
                avgY = event.values[1];
                avgZ = event.values[2];
                aquisitions = 1;
            }
            if (lastReading + period <= event.timestamp) {
                //Average/waiting period is over
                //Append the data to available buffers
                if (dataX != null)
                    dataX.append(avgX/aquisitions);
                if (dataY != null)
                    dataY.append(avgY/aquisitions);
                if (dataZ != null)
                    dataZ.append(avgZ/aquisitions);
                if (dataT != null)
                    dataT.append((event.timestamp-t0)*1e-9); //We want seconds since t0
                //Reset averaging
                avgX = 0.;
                avgY = 0.;
                avgZ = 0.;
                lastReading = event.timestamp;
                aquisitions = 0;
            }
        }
    }
}