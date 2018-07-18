
package com.clj.fastble.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleIndicateCallback;
import com.clj.fastble.callback.BleMtuChangedCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleReadCallback;
import com.clj.fastble.callback.BleRssiCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleMsg;
import com.clj.fastble.data.BleWriteState;
import com.clj.fastble.exception.GattException;
import com.clj.fastble.exception.OtherException;
import com.clj.fastble.exception.TimeoutException;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleConnector {
    private static final String UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR = "00002902-0000-1000-8000-00805f9b34fb";
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattService mGattService;
    private BluetoothGattCharacteristic mCharacteristic;
    private BleBluetooth mBleBluetooth;
    private Handler mMainThreadHandler;
    private boolean mIsWriteSuc =false;
    BleConnector(BleBluetooth bleBluetooth) {
        this.mBleBluetooth = bleBluetooth;
        this.mBluetoothGatt = bleBluetooth.getBluetoothGatt();


        this.mMainThreadHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case BleMsg.MSG_CHA_NOTIFY_START: {
                        BleNotifyCallback notifyCallback = (BleNotifyCallback) msg.obj;
                        if (notifyCallback != null)
                            notifyCallback.onNotifyFailure(new TimeoutException());
                        break;
                    }

                    case BleMsg.MSG_CHA_NOTIFY_RESULT: {
                        notifyMsgInit();

                        BleNotifyCallback notifyCallback = (BleNotifyCallback) msg.obj;
                        Bundle bundle = msg.getData();
                        int status = bundle.getInt(BleMsg.KEY_NOTIFY_BUNDLE_STATUS);
                        if (notifyCallback != null) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                notifyCallback.onNotifySuccess();
                            } else {
                                notifyCallback.onNotifyFailure(new GattException(status));
                            }
                        }
                        break;
                    }

                    case BleMsg.MSG_CHA_NOTIFY_DATA_CHANGE: {
                        BleNotifyCallback notifyCallback = (BleNotifyCallback) msg.obj;
                        Bundle bundle = msg.getData();
                        byte[] value = bundle.getByteArray(BleMsg.KEY_NOTIFY_BUNDLE_VALUE);
                        if (notifyCallback != null) {
                            notifyCallback.onCharacteristicChanged(value);
                        }
                        break;
                    }

                    case BleMsg.MSG_CHA_INDICATE_START: {
                        BleIndicateCallback indicateCallback = (BleIndicateCallback) msg.obj;
                        if (indicateCallback != null)
                            indicateCallback.onIndicateFailure(new TimeoutException());
                        break;
                    }

                    case BleMsg.MSG_CHA_INDICATE_RESULT: {
                        indicateMsgInit();

                        BleIndicateCallback indicateCallback = (BleIndicateCallback) msg.obj;
                        Bundle bundle = msg.getData();
                        int status = bundle.getInt(BleMsg.KEY_INDICATE_BUNDLE_STATUS);
                        if (indicateCallback != null) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                indicateCallback.onIndicateSuccess();
                            } else {
                                indicateCallback.onIndicateFailure(new GattException(status));
                            }
                        }
                        break;
                    }

                    case BleMsg.MSG_CHA_INDICATE_DATA_CHANGE: {
                        BleIndicateCallback indicateCallback = (BleIndicateCallback) msg.obj;
                        Bundle bundle = msg.getData();
                        byte[] value = bundle.getByteArray(BleMsg.KEY_INDICATE_BUNDLE_VALUE);
                        if (indicateCallback != null) {
                            indicateCallback.onCharacteristicChanged(value);
                        }
                        break;
                    }

                    case BleMsg.MSG_CHA_WRITE_START: {
                        BleWriteCallback writeCallback = (BleWriteCallback) msg.obj;
                        if (writeCallback != null) {
                            writeCallback.onWriteFailure(new TimeoutException());
                        }
                        break;
                    }

                    case BleMsg.MSG_CHA_WRITE_RESULT: {
                        writeMsgInit();

                        BleWriteCallback writeCallback = (BleWriteCallback) msg.obj;
                        Bundle bundle = msg.getData();
                        int status = bundle.getInt(BleMsg.KEY_WRITE_BUNDLE_STATUS);
                        byte[] value = bundle.getByteArray(BleMsg.KEY_WRITE_BUNDLE_VALUE);
                        if (writeCallback != null) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                mIsWriteSuc = true;
                                writeCallback.onWriteSuccess(BleWriteState.DATA_WRITE_SINGLE, BleWriteState.DATA_WRITE_SINGLE, value);
                            } else {
                                mIsWriteSuc = false;
                                writeCallback.onWriteFailure(new GattException(status));
                            }
                        }

                        break;
                    }

                    case BleMsg.MSG_CHA_READ_START: {
                        BleReadCallback readCallback = (BleReadCallback) msg.obj;
                        if (readCallback != null)
                            readCallback.onReadFailure(new TimeoutException());
                        break;
                    }

                    case BleMsg.MSG_CHA_READ_RESULT: {
                        readMsgInit();

                        BleReadCallback readCallback = (BleReadCallback) msg.obj;
                        Bundle bundle = msg.getData();
                        int status = bundle.getInt(BleMsg.KEY_READ_BUNDLE_STATUS);
                        byte[] value = bundle.getByteArray(BleMsg.KEY_READ_BUNDLE_VALUE);
                        if (readCallback != null) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                readCallback.onReadSuccess(value);
                            } else {
                                readCallback.onReadFailure(new GattException(status));
                            }
                        }
                        break;
                    }

                    case BleMsg.MSG_READ_RSSI_START: {
                        BleRssiCallback rssiCallback = (BleRssiCallback) msg.obj;
                        if (rssiCallback != null)
                            rssiCallback.onRssiFailure(new TimeoutException());
                        break;
                    }

                    case BleMsg.MSG_READ_RSSI_RESULT: {
                        rssiMsgInit();

                        BleRssiCallback rssiCallback = (BleRssiCallback) msg.obj;
                        Bundle bundle = msg.getData();
                        int status = bundle.getInt(BleMsg.KEY_READ_RSSI_BUNDLE_STATUS);
                        int value = bundle.getInt(BleMsg.KEY_READ_RSSI_BUNDLE_VALUE);
                        if (rssiCallback != null) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                rssiCallback.onRssiSuccess(value);
                            } else {
                                rssiCallback.onRssiFailure(new GattException(status));
                            }
                        }
                        break;
                    }

                    case BleMsg.MSG_SET_MTU_START: {
                        BleMtuChangedCallback mtuChangedCallback = (BleMtuChangedCallback) msg.obj;
                        if (mtuChangedCallback != null)
                            mtuChangedCallback.onSetMTUFailure(new TimeoutException());
                        break;
                    }

                    case BleMsg.MSG_SET_MTU_RESULT: {
                        mtuChangedMsgInit();

                        BleMtuChangedCallback mtuChangedCallback = (BleMtuChangedCallback) msg.obj;
                        Bundle bundle = msg.getData();
                        int status = bundle.getInt(BleMsg.KEY_SET_MTU_BUNDLE_STATUS);
                        int value = bundle.getInt(BleMsg.KEY_SET_MTU_BUNDLE_VALUE);
                        if (mtuChangedCallback != null) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                mtuChangedCallback.onMtuChanged(value);
                            } else {
                                mtuChangedCallback.onSetMTUFailure(new GattException(status));
                            }
                        }
                        break;
                    }
                }
            }
        };

    }

    private BleConnector withUUID(UUID serviceUUID, UUID characteristicUUID) {
        if (serviceUUID != null && mBluetoothGatt != null) {
            mGattService = mBluetoothGatt.getService(serviceUUID);
        }
        if (mGattService != null && characteristicUUID != null) {
            mCharacteristic = mGattService.getCharacteristic(characteristicUUID);
        }
        return this;
    }

    private static final String TAG = "BleConnector";
    public BleConnector withUUIDString(String serviceUUID, String characteristicUUID) {

        return withUUID(formUUID(serviceUUID), formUUID(characteristicUUID));
    }

    private UUID formUUID(String uuid) {
        return uuid == null ? null : UUID.fromString(uuid);
    }


     /*------------------------------- main operation ----------------------------------- */


    /**
     * notify
     */
    public void enableCharacteristicNotify(BleNotifyCallback bleNotifyCallback, String uuid_notify) {
        if (mCharacteristic != null
                && (mCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {

            handleCharacteristicNotifyCallback(bleNotifyCallback, uuid_notify);
            setCharacteristicNotification(mBluetoothGatt, mCharacteristic, true, bleNotifyCallback);
        } else {
            if (bleNotifyCallback != null)
                bleNotifyCallback.onNotifyFailure(new OtherException("this characteristic not support notify!"));
        }
    }

    /**
     * stop notify
     */
    public boolean disableCharacteristicNotify() {
        if (mCharacteristic != null
                && (mCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            return setCharacteristicNotification(mBluetoothGatt, mCharacteristic, false, null);
        } else {
            return false;
        }
    }

    /**
     * notify setting
     */
    private boolean setCharacteristicNotification(BluetoothGatt gatt,
                                                  BluetoothGattCharacteristic characteristic,
                                                  boolean enable,
                                                  BleNotifyCallback bleNotifyCallback) {
        if (gatt == null || characteristic == null) {
            notifyMsgInit();
            if (bleNotifyCallback != null)
                bleNotifyCallback.onNotifyFailure(new OtherException("gatt or characteristic equal null"));
            return false;
        }

        boolean success1 = gatt.setCharacteristicNotification(characteristic, enable);
        if (!success1) {
            notifyMsgInit();
            if (bleNotifyCallback != null)
                bleNotifyCallback.onNotifyFailure(new OtherException("gatt setCharacteristicNotification fail"));
            return false;
        }

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(formUUID(UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR));
        if (descriptor == null) {
            notifyMsgInit();
            if (bleNotifyCallback != null)
                bleNotifyCallback.onNotifyFailure(new OtherException("descriptor equals null"));
            return false;
        } else {
            descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE :
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            boolean success2 = gatt.writeDescriptor(descriptor);
            if (!success2) {
                notifyMsgInit();
                if (bleNotifyCallback != null)
                    bleNotifyCallback.onNotifyFailure(new OtherException("gatt writeDescriptor fail"));
            }
            return true;
        }
    }

    /**
     * indicate
     */
    public void enableCharacteristicIndicate(BleIndicateCallback bleIndicateCallback, String uuid_indicate) {
        if (mCharacteristic != null
                && (mCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            handleCharacteristicIndicateCallback(bleIndicateCallback, uuid_indicate);
            setCharacteristicIndication(mBluetoothGatt, mCharacteristic, true, bleIndicateCallback);
        } else {
            if (bleIndicateCallback != null)
                bleIndicateCallback.onIndicateFailure(new OtherException("this characteristic not support indicate!"));
        }
    }


    /**
     * stop indicate
     */
    public boolean disableCharacteristicIndicate() {
        if (mCharacteristic != null
                && (mCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            return setCharacteristicIndication(mBluetoothGatt, mCharacteristic, false, null);
        } else {
            return false;
        }
    }

    /**
     * indicate setting
     */
    private boolean setCharacteristicIndication(BluetoothGatt gatt,
                                                BluetoothGattCharacteristic characteristic,
                                                boolean enable,
                                                BleIndicateCallback bleIndicateCallback) {
        if (gatt == null || characteristic == null) {
            indicateMsgInit();
            if (bleIndicateCallback != null)
                bleIndicateCallback.onIndicateFailure(new OtherException("gatt or characteristic equal null"));
            return false;
        }

        boolean success1 = gatt.setCharacteristicNotification(characteristic, enable);
        if (!success1) {
            indicateMsgInit();
            if (bleIndicateCallback != null)
                bleIndicateCallback.onIndicateFailure(new OtherException("gatt setCharacteristicNotification fail"));
            return false;
        }

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(formUUID(UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR));
        if (descriptor == null) {
            indicateMsgInit();
            if (bleIndicateCallback != null)
                bleIndicateCallback.onIndicateFailure(new OtherException("descriptor equals null"));
            return false;
        } else {
            descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE :
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            boolean success2 = gatt.writeDescriptor(descriptor);
            if (!success2) {
                indicateMsgInit();
                if (bleIndicateCallback != null)
                    bleIndicateCallback.onIndicateFailure(new OtherException("gatt writeDescriptor fail"));
            }
            return success2;
        }
    }

    /**
     * write
     */
    public void writeCharacteristic(byte[] data, BleWriteCallback bleWriteCallback, String uuid_write) {
        Log.d(TAG, "start write thread = " + Thread.currentThread().getName());
            if (data == null || data.length <= 0) {
                if (bleWriteCallback != null)
                    bleWriteCallback.onWriteFailure(new OtherException("the data to be written is empty"));
                return;
            }

            if (mCharacteristic == null
                    || (mCharacteristic.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0) {
                if (bleWriteCallback != null)
                    bleWriteCallback.onWriteFailure(new OtherException("this characteristic not support write!"));
                return;
            }
            if (mCharacteristic.setValue(data)) {
                handleCharacteristicWriteCallback(bleWriteCallback, uuid_write);
                boolean isWriteSuccess = mBluetoothGatt.writeCharacteristic(mCharacteristic);
                if (!isWriteSuccess) {
                    Log.w(TAG, "isWriteFail");
                    writeMsgInit();
                    if (bleWriteCallback != null)
                        bleWriteCallback.onWriteFailure(new OtherException("gatt writeCharacteristic fail"));
                }

            } else {
                if (bleWriteCallback != null)
                    bleWriteCallback.onWriteFailure(new OtherException("Updates the locally stored value of this characteristic fail"));
            }

        Log.d(TAG, "end write thread = " + Thread.currentThread().getName());
    }

    /**
     * read
     */
    public void readCharacteristic(BleReadCallback bleReadCallback, String uuid_read) {
        if (mCharacteristic != null
                && (mCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {

            handleCharacteristicReadCallback(bleReadCallback, uuid_read);
            if (!mBluetoothGatt.readCharacteristic(mCharacteristic)) {
                readMsgInit();
                if (bleReadCallback != null)
                    bleReadCallback.onReadFailure(new OtherException("gatt readCharacteristic fail"));
            }
        } else {
            if (bleReadCallback != null)
                bleReadCallback.onReadFailure(new OtherException("this characteristic not support read!"));
        }
    }

    /**
     * rssi
     */
    public void readRemoteRssi(BleRssiCallback bleRssiCallback) {
        handleRSSIReadCallback(bleRssiCallback);
        if (!mBluetoothGatt.readRemoteRssi()) {
            rssiMsgInit();
            if (bleRssiCallback != null)
                bleRssiCallback.onRssiFailure(new OtherException("gatt readRemoteRssi fail"));
        }
    }

    /**
     * set mtu
     */
    public void setMtu(int requiredMtu, BleMtuChangedCallback bleMtuChangedCallback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            handleSetMtuCallback(bleMtuChangedCallback);
            if (!mBluetoothGatt.requestMtu(requiredMtu)) {
                mtuChangedMsgInit();
                if (bleMtuChangedCallback != null)
                    bleMtuChangedCallback.onSetMTUFailure(new OtherException("gatt requestMtu fail"));
            }
        } else {
            if (bleMtuChangedCallback != null)
                bleMtuChangedCallback.onSetMTUFailure(new OtherException("API level lower than 21"));
        }
    }


    /**************************************** Handle call back ******************************************/

    /**
     * notify
     */
    private void handleCharacteristicNotifyCallback(BleNotifyCallback bleNotifyCallback,
                                                    String uuid_notify) {
        if (bleNotifyCallback != null) {
            notifyMsgInit();
            bleNotifyCallback.setKey(uuid_notify);
            bleNotifyCallback.setHandler(mMainThreadHandler);
            mBleBluetooth.addNotifyCallback(uuid_notify, bleNotifyCallback);
            mMainThreadHandler.sendMessageDelayed(
                    mMainThreadHandler.obtainMessage(BleMsg.MSG_CHA_NOTIFY_START, bleNotifyCallback),
                    BleManager.getInstance().getOperateTimeout());
        }
    }

    /**
     * indicate
     */
    private void handleCharacteristicIndicateCallback(BleIndicateCallback bleIndicateCallback,
                                                      String uuid_indicate) {
        if (bleIndicateCallback != null) {
            indicateMsgInit();
            bleIndicateCallback.setKey(uuid_indicate);
            bleIndicateCallback.setHandler(mMainThreadHandler);
            mBleBluetooth.addIndicateCallback(uuid_indicate, bleIndicateCallback);
            mMainThreadHandler.sendMessageDelayed(
                    mMainThreadHandler.obtainMessage(BleMsg.MSG_CHA_INDICATE_START, bleIndicateCallback),
                    BleManager.getInstance().getOperateTimeout());
        }
    }

    /**
     * write
     */
    private void handleCharacteristicWriteCallback(BleWriteCallback bleWriteCallback,
                                                   String uuid_write) {
        if (bleWriteCallback != null) {
            writeMsgInit();
            bleWriteCallback.setKey(uuid_write);
            bleWriteCallback.setHandler(mMainThreadHandler);
            mBleBluetooth.addWriteCallback(uuid_write, bleWriteCallback);
            mMainThreadHandler.sendMessageDelayed(
                    mMainThreadHandler.obtainMessage(BleMsg.MSG_CHA_WRITE_START, bleWriteCallback),
                    BleManager.getInstance().getOperateTimeout());
        }
    }

    /**
     * read
     */
    private void handleCharacteristicReadCallback(BleReadCallback bleReadCallback,
                                                  String uuid_read) {
        if (bleReadCallback != null) {
            readMsgInit();
            bleReadCallback.setKey(uuid_read);
            bleReadCallback.setHandler(mMainThreadHandler);
            mBleBluetooth.addReadCallback(uuid_read, bleReadCallback);
            mMainThreadHandler.sendMessageDelayed(
                    mMainThreadHandler.obtainMessage(BleMsg.MSG_CHA_READ_START, bleReadCallback),
                    BleManager.getInstance().getOperateTimeout());
        }
    }

    /**
     * rssi
     */
    private void handleRSSIReadCallback(BleRssiCallback bleRssiCallback) {
        if (bleRssiCallback != null) {
            rssiMsgInit();
            bleRssiCallback.setHandler(mMainThreadHandler);
            mBleBluetooth.addRssiCallback(bleRssiCallback);
            mMainThreadHandler.sendMessageDelayed(
                    mMainThreadHandler.obtainMessage(BleMsg.MSG_READ_RSSI_START, bleRssiCallback),
                    BleManager.getInstance().getOperateTimeout());
        }
    }

    /**
     * set mtu
     */
    private void handleSetMtuCallback(BleMtuChangedCallback bleMtuChangedCallback) {
        if (bleMtuChangedCallback != null) {
            mtuChangedMsgInit();
            bleMtuChangedCallback.setHandler(mMainThreadHandler);
            mBleBluetooth.addMtuChangedCallback(bleMtuChangedCallback);
            mMainThreadHandler.sendMessageDelayed(
                    mMainThreadHandler.obtainMessage(BleMsg.MSG_SET_MTU_START, bleMtuChangedCallback),
                    BleManager.getInstance().getOperateTimeout());
        }
    }

    public void notifyMsgInit() {
        mMainThreadHandler.removeMessages(BleMsg.MSG_CHA_NOTIFY_START);
    }

    public void indicateMsgInit() {
        mMainThreadHandler.removeMessages(BleMsg.MSG_CHA_INDICATE_START);
    }

    public void writeMsgInit() {
        mMainThreadHandler.removeMessages(BleMsg.MSG_CHA_WRITE_START);
    }

    public void readMsgInit() {
        mMainThreadHandler.removeMessages(BleMsg.MSG_CHA_READ_START);
    }

    public void rssiMsgInit() {
        mMainThreadHandler.removeMessages(BleMsg.MSG_READ_RSSI_START);
    }

    public void mtuChangedMsgInit() {
        mMainThreadHandler.removeMessages(BleMsg.MSG_SET_MTU_START);
    }

}