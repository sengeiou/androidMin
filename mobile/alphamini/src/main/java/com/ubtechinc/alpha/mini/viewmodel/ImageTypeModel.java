package com.ubtechinc.alpha.mini.viewmodel;

/**
 * Created by Administrator on 2017/10/30.
 */

import java.io.Serializable;

public class ImageTypeModel  implements Serializable{

    private static final long serialVersionUID = -8733860560384712021L;
    private String typeDate;
    private int positon;
    public String getTypeDate() {
        return typeDate;
    }
    public void setTypeDate(String typeDate) {
        this.typeDate = typeDate;
    }
    public int getPositon() {
        return positon;
    }
    public void setPositon(int positon) {
        this.positon = positon;
    }

}