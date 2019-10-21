package com.zmm.usbserialforandroidtest;

import android.widget.BaseAdapter;

import java.util.ArrayList;

class BurnThread extends Thread {

    byte[] bytesB = new byte[64];
    int anInt = 0;
    int s ;
    byte[]bytes;
    private byte[][] Data;
    private UsbService usbService;
    private int step = 0;
    private boolean isRun = false;
    State state;
    public BurnThread(UsbService usbService,String data[], int s,State state) {
        this.state = state;
        this.s = s;
        this.usbService = usbService;
        Data=HexUtils.hexStr2Bytess(data);
    }
    public BurnThread(UsbService usbService, byte[][] Data, int s,State state) {
        this.state = state;
        this.s = s;
        this.usbService = usbService;
        this.Data = Data;
    }
    public void setS(int s) {
        this.s = s;
    }
    public void setState(State state) {
        this.state = state;
    }

    public void setData(String data[]) {
        Data=HexUtils.hexStr2Bytess(data);
    }

    public boolean isRun() {
        return isRun;
    }
    @Override
    public void run() {
        isRun=true;
        BurnActivity.w("isRun && step < Data.length="+(isRun && step < Data.length));
        while (isRun && step < Data.length) {
            byte[] aByte=HexUtils.TotalToBytes(s,256,4);
            bytes=Data[step];
            System.arraycopy(aByte,0,bytes,4,aByte.length);
            usbService.write(bytes);
            s+=2;
            try {
                sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        isRun=false;
        state.end();
    }
    public void write(byte[] data){
        if (isRun()&&step<Data.length) {

            if (anInt==0){
                byte[]NowData=getNowData();
                byte[] b=adaptation(data);
                int l=isSign(b, HexUtils.getTotal(NowData, 1),4);
                if (l>=0){
                    System.arraycopy(b, l, bytesB, anInt, b.length-l>64?64:b.length-l);
                    anInt = b.length-l;
                }
            }else if (anInt<=bytesB.length){
                int l= data.length+anInt>bytesB.length?data.length-(data.length+anInt-bytesB.length):data.length;
                System.arraycopy(data, 0, bytesB, anInt,l);
                anInt += l;
            }else {
                BurnActivity.w("anInt="+anInt);
                anInt=0;

            }
            if (anInt == bytesB.length) {
                BurnActivity.w("bytesB="+HexUtils.byte2HexStr(bytesB,bytesB.length));
                BurnActivity.w("发送数组大小="+HexUtils.byte2HexStr(HexUtils.TotalToBytes(HexUtils.getTotal(getNowData(), 1),256,4),4)+
                " 对方验证的数据大小"+HexUtils.byte2HexStr(HexUtils.arraycopy(bytesB,0,4),4));
                BurnActivity.w(HexUtils.getTotal(HexUtils.TotalToBytes(HexUtils.getTotal(getNowData(), 1),256,4), 4)+"      "+HexUtils.getTotal(HexUtils.arraycopy(bytesB,0,4), 4));
                if ( HexUtils.getTotal(HexUtils.TotalToBytes(HexUtils.getTotal(getNowData(), 1),256,4), 4)==
                        HexUtils.getTotal(HexUtils.arraycopy(bytesB,0,4), 4)){
                    NextStep();
                }
                anInt = 0;
            }

        }
    }
    byte[]Bytes1=new byte[0];
    byte[] adaptation(byte[] data){
        byte[]newByte=new byte[data.length+Bytes1.length];
        System.arraycopy(Bytes1,0,newByte,0,Bytes1.length);
        System.arraycopy(data,0,newByte,Bytes1.length,data.length);
        return newByte;

    }
     public int  isSign( byte[] data,int s,int l){
       BurnActivity.w("isSign data="+  HexUtils.byte2HexStr(data,data.length));
        int x=-1;
        if (data.length>=l){
            for (int i=0;i<data.length-l;i++){
                byte[]bytes=new byte[l];
                System.arraycopy(data,i,bytes,0,bytes.length);
                byte[]bytes1=HexUtils.TotalToBytes(s,256,l);
                BurnActivity.w("bytes="+HexUtils.byte2HexStr(bytes,bytes.length)+" bytes1="+HexUtils.byte2HexStr(bytes1,bytes1.length)+" "+
                        bytes.equals(bytes1)+" "+
                        ( HexUtils.getTotal(bytes)==HexUtils.getTotal(bytes1)));
                if ( HexUtils.getTotal(bytes)==HexUtils.getTotal(bytes1)){
                    x=i;
                    break;
                }
            }
            Bytes1=new byte[l-1];
            System.arraycopy(data,data.length-Bytes1.length,Bytes1,0,Bytes1.length);
        }else {
            Bytes1=data;
        }
        return x;
    }

    public byte[] getNowData() {
        return bytes;
    }


    public void Stop() {
        isRun = false;
    }

    public void NextStep() {
        BurnActivity.w("NextStep"+step);
        step++;
    }
    public interface State{
        void end();
    }
}
