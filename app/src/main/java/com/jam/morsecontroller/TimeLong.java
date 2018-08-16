package com.jam.morsecontroller;

/**
 * Created by Jam on 30.04.2018.
 */

public class TimeLong {
    private long time = 0;
    private long timeFromConstructor = 0;
    TimeLong() {
        this.time = 10000;
        timeFromConstructor = 10000;
    }
    TimeLong(long time) {
        this.time = time;
        timeFromConstructor = this.time;
    }

    public long getTime() {
        return this.time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void resetTime() {
        this.time = timeFromConstructor;
    }

    public double getTimeInSec(){
        double timeInSec = ((double)this.time) / 10;
        return timeInSec;
    }
}
