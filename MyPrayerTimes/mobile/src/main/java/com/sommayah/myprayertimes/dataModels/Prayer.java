package com.sommayah.myprayertimes.dataModels;

/**
 * Created by sommayahsoliman on 3/10/16.
 */
public class Prayer {
    private String name;
    private String time;
    private boolean tomorrow;

    public Prayer(String name, String time){
        this.name = name;
        this.time = time;
        this.tomorrow = false;
    }

    public Prayer(String name, String time, boolean tomorrow){
        this.name = name;
        this.time = time;
        this.tomorrow = tomorrow;
    }

    public String getName(){return name;}
    public String getTime(){return time;}
    public void setTomorrow(boolean tomorrow){
        this.tomorrow = tomorrow;
    }
    public boolean getTomorrow(){return tomorrow;}
}
