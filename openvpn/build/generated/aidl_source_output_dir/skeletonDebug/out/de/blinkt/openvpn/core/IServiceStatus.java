/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package de.blinkt.openvpn.core;
public interface IServiceStatus extends android.os.IInterface
{
  /** Default implementation for IServiceStatus. */
  public static class Default implements de.blinkt.openvpn.core.IServiceStatus
  {
    /**
     * Registers to receive OpenVPN Status Updates and gets a
     * ParcelFileDescript back that contains the log up to that point
     */
    @Override public android.os.ParcelFileDescriptor registerStatusCallback(de.blinkt.openvpn.core.IStatusCallbacks cb) throws android.os.RemoteException
    {
      return null;
    }
    /** Remove a previously registered callback interface. */
    @Override public void unregisterStatusCallback(de.blinkt.openvpn.core.IStatusCallbacks cb) throws android.os.RemoteException
    {
    }
    /** Returns the last connedcted VPN */
    @Override public java.lang.String getLastConnectedVPN() throws android.os.RemoteException
    {
      return null;
    }
    /** Sets a cached password */
    @Override public void setCachedPassword(java.lang.String uuid, int type, java.lang.String password) throws android.os.RemoteException
    {
    }
    /** Gets the traffic history */
    @Override public de.blinkt.openvpn.core.TrafficHistory getTrafficHistory() throws android.os.RemoteException
    {
      return null;
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements de.blinkt.openvpn.core.IServiceStatus
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an de.blinkt.openvpn.core.IServiceStatus interface,
     * generating a proxy if needed.
     */
    public static de.blinkt.openvpn.core.IServiceStatus asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof de.blinkt.openvpn.core.IServiceStatus))) {
        return ((de.blinkt.openvpn.core.IServiceStatus)iin);
      }
      return new de.blinkt.openvpn.core.IServiceStatus.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
        data.enforceInterface(descriptor);
      }
      switch (code)
      {
        case INTERFACE_TRANSACTION:
        {
          reply.writeString(descriptor);
          return true;
        }
      }
      switch (code)
      {
        case TRANSACTION_registerStatusCallback:
        {
          de.blinkt.openvpn.core.IStatusCallbacks _arg0;
          _arg0 = de.blinkt.openvpn.core.IStatusCallbacks.Stub.asInterface(data.readStrongBinder());
          android.os.ParcelFileDescriptor _result = this.registerStatusCallback(_arg0);
          reply.writeNoException();
          _Parcel.writeTypedObject(reply, _result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_unregisterStatusCallback:
        {
          de.blinkt.openvpn.core.IStatusCallbacks _arg0;
          _arg0 = de.blinkt.openvpn.core.IStatusCallbacks.Stub.asInterface(data.readStrongBinder());
          this.unregisterStatusCallback(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getLastConnectedVPN:
        {
          java.lang.String _result = this.getLastConnectedVPN();
          reply.writeNoException();
          reply.writeString(_result);
          break;
        }
        case TRANSACTION_setCachedPassword:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          int _arg1;
          _arg1 = data.readInt();
          java.lang.String _arg2;
          _arg2 = data.readString();
          this.setCachedPassword(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getTrafficHistory:
        {
          de.blinkt.openvpn.core.TrafficHistory _result = this.getTrafficHistory();
          reply.writeNoException();
          _Parcel.writeTypedObject(reply, _result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements de.blinkt.openvpn.core.IServiceStatus
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      /**
       * Registers to receive OpenVPN Status Updates and gets a
       * ParcelFileDescript back that contains the log up to that point
       */
      @Override public android.os.ParcelFileDescriptor registerStatusCallback(de.blinkt.openvpn.core.IStatusCallbacks cb) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.os.ParcelFileDescriptor _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(cb);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerStatusCallback, _data, _reply, 0);
          _reply.readException();
          _result = _Parcel.readTypedObject(_reply, android.os.ParcelFileDescriptor.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /** Remove a previously registered callback interface. */
      @Override public void unregisterStatusCallback(de.blinkt.openvpn.core.IStatusCallbacks cb) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(cb);
          boolean _status = mRemote.transact(Stub.TRANSACTION_unregisterStatusCallback, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /** Returns the last connedcted VPN */
      @Override public java.lang.String getLastConnectedVPN() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        java.lang.String _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getLastConnectedVPN, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readString();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /** Sets a cached password */
      @Override public void setCachedPassword(java.lang.String uuid, int type, java.lang.String password) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(uuid);
          _data.writeInt(type);
          _data.writeString(password);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setCachedPassword, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /** Gets the traffic history */
      @Override public de.blinkt.openvpn.core.TrafficHistory getTrafficHistory() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        de.blinkt.openvpn.core.TrafficHistory _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getTrafficHistory, _data, _reply, 0);
          _reply.readException();
          _result = _Parcel.readTypedObject(_reply, de.blinkt.openvpn.core.TrafficHistory.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
    }
    static final int TRANSACTION_registerStatusCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_unregisterStatusCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_getLastConnectedVPN = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_setCachedPassword = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_getTrafficHistory = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
  }
  public static final java.lang.String DESCRIPTOR = "de.blinkt.openvpn.core.IServiceStatus";
  /**
   * Registers to receive OpenVPN Status Updates and gets a
   * ParcelFileDescript back that contains the log up to that point
   */
  public android.os.ParcelFileDescriptor registerStatusCallback(de.blinkt.openvpn.core.IStatusCallbacks cb) throws android.os.RemoteException;
  /** Remove a previously registered callback interface. */
  public void unregisterStatusCallback(de.blinkt.openvpn.core.IStatusCallbacks cb) throws android.os.RemoteException;
  /** Returns the last connedcted VPN */
  public java.lang.String getLastConnectedVPN() throws android.os.RemoteException;
  /** Sets a cached password */
  public void setCachedPassword(java.lang.String uuid, int type, java.lang.String password) throws android.os.RemoteException;
  /** Gets the traffic history */
  public de.blinkt.openvpn.core.TrafficHistory getTrafficHistory() throws android.os.RemoteException;
  /** @hide */
  static class _Parcel {
    static private <T> T readTypedObject(
        android.os.Parcel parcel,
        android.os.Parcelable.Creator<T> c) {
      if (parcel.readInt() != 0) {
          return c.createFromParcel(parcel);
      } else {
          return null;
      }
    }
    static private <T extends android.os.Parcelable> void writeTypedObject(
        android.os.Parcel parcel, T value, int parcelableFlags) {
      if (value != null) {
        parcel.writeInt(1);
        value.writeToParcel(parcel, parcelableFlags);
      } else {
        parcel.writeInt(0);
      }
    }
  }
}
