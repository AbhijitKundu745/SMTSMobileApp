package com.psllab.smtsmobileapp.helper;

public class APIConstants {
    //http://psltestapi.azurewebsites.net/
    public static final String M_USER_LOGIN = "/PDA/Login";
    public static final String M_UPLOAD_INVENTORY1 = "/PDA/TransactionMobile";
    public static final String M_UPLOAD_INVENTORY = "/PDA/InsertInventoryData";
    public static final String M_INVENTORY_STATUS = "/PDA/GetInventoryDataStatus";
    public static final String M_GET_ASSETS = "/PDA/GetAssets";
    public static final String M_ASSET_REGISTRATION = "/PDA/RegisterAssetMobile";
    public static final String M_GET_LOCATION_MASTER = "/PDA/GetAllLocations";
    public static final String M_UPLOAD_INOUT = "/PDA/InsertTransactionDetails";
    public static final String M_UPLOAD_INOUT_KIT_ACTIVITY = "/PDA/InsertTransactionDetails1";
    public static final String M_GET_KIT_OUT_LIFE_STATUS = "/PDA/GetKitOutLifeStatus";
    public static final String M_GET_ASSET_MASTER = "/PDA/GetAllAssetsMobile?tenantID=";
    public static final String M_GET_ROOM_MASTER = "/PDA/GetAllRooms?tenantID=";
    public static final String M_GET_LOST_ASSET_MASTER = "/PDA/GetLostAssets?tenantID=";

    public static final int API_TIMEOUT = 120;

    public static final String K_STATUS = "status";
    public static final String K_MESSAGE = "message";
    public static final String K_DATA = "data";
    public static final String K_KIT_OUT_STATUS = "KitOutLifeStatus";
    public static final String K_KIT_OUT_LIFE = "OutLife";

    public static final String K_USER = "UserName";

    public static final String K_PASSWORD = "Password";
    public static final String K_DEVICE_ID = "ClientDeviceID";

    public static final String K_READER_ID = "ReaderIP";
    public static final String K_ANTENA_ID = "AntenaID";
    public static final String K_RSSI = "RSSI";
    public static final String K_TAG_ID = "TagId";
    public static final String K_SERIAL_NUMBER = "SerialNO";
    public static final String K_TAG_TYPE = "TagType";
    public static final String K_TRANS_LOCATION_ID = "LocationID";
    public static final String K_TOUCH_POINT_ID = "TouchPointID";
    public static final String K_TOUCH_POINT_TYPE = "TouchPointType";
    public static final String K_TTRANSACTION_DATE_TIME = "TransDateTime";
    public static final String K_USER_ID = "UserID";
    public static final String K_CUSTOMER_ID = "CustomerID";
    public static final String K_COMPANY_ID = "";
    public static final String K_COMPANY_CODE = "CompanyCode";
    public static final String K_TAG_ACCESS_PASSWORD = "TagPassword";
    public static final String K_IS_LOG_REQUIRED = "ISLogRequired";
    public static final String K_CURRENT_ACCESS_PASSWORD = "";

    public static final String K_ROOM_MASTER = "Room";
    public static final String K_VENDOR_MASTER = "Vendor";
    public static final String K_LOCATION_MASTER = "Location";
    public static final String K_AUTOCLAVE_LOCATION_MASTER = "Autoclave_Location";
    public static final String K_ASSETTYPE_MASTER = "AssetType";

    public static final String K_VENDOR_ID = "VendorID";
    public static final String K_LOCATION_ID = "LocationId";
    public static final String K_ROOM_ID = "RoomID";
    public static final String K_ROOM_RFID = "TagId";
    public static final String K_UID = "UID";
    public static final String K_VENDOR_NAME = "LocationName";
    public static final String K_LOCATION_NAME = "LocationName";
    public static final String K_ROOM_NAME = "RoomName";
    public static final String K_ASSET_TYPE_ID = "ATypeID";
    public static final String K_ASSET_TYPE_NAME = "AssetName";
    //public static final String K_TOUCH_POINT_ID = "TouchpointID";

    public static final String K_ASSET_ID = "AssetID";
    public static final String K_ASSET_NAME = "AName";
    public static final String K_ASSET_SERIAL_NUMBER = "ASerialNo";

    public static final String K_TAG_TID = "ATagTid";
    public static final String K_CURRENT_TAG_ID = "ATagId";
    public static final String K_PREVIOUS_TAG_ID = "OldATagId";

    public static final String K_IS_REGISTERED = "IsRegistered";
    public static final String K_INVENTORY_TYPE = "ActivityType";
    public static final String K_INVENTORY_COUNT = "Count";
    public static final String K_INVENTORY_TIME = "TimeTaken";
    public static final String K_INVENTORY_START_DATE_TIME = "StartDate";
    public static final String K_INVENTORY_END_DATE_TIME = "EndDate";
    public static final String K_INVENTORY_DATE_TIME = "DateTime";


    public static final String K_DASHBOARD_ARRAY = "Dashboard";
    public static final String K_DASHBOARD_MENU_ID = "Menu_ID";
    public static final String K_DASHBOARD_MENU_NAME = "Menu_Name";
    public static final String K_DASHBOARD_MENU_IMAGE = "Menu_Image";
    public static final String K_DASHBOARD_MENU_ACTIVE = "Menu_Is_Active";
    public static final String K_DASHBOARD_MENU_SEQUENCE = "Menu_Sequence";


    public static final String K_ACTION_SYNC = "SYNC";
    public static final String K_ACTION_INVENTORY = "INV";
    public static final String K_ACTION_MAPPING = "MAP";
    public static final String K_ACTION_CHECKIN = "IN";
    public static final String K_ACTION_CHECKOUT = "OUT";
    public static final String K_ACTION_ROOM_CHECKOUT = "ROOMOUT";
    public static final String K_ACTION_TRACKPOINT = "TRACKPOINT";
    public static final String K_ACTION_SECURITY_OUT = "SECURITYOUT";
}
