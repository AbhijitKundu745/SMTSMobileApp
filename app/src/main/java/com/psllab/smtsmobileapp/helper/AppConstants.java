package com.psllab.smtsmobileapp.helper;


import com.psllab.smtsmobileapp.databases.LocationMaster;

import java.util.ArrayList;
import java.util.List;

public class AppConstants {

    public static final String ASSET_TYPE_SPLIT_DATA = "PSLLAB";
    public static String SEARCH = "30361F4B3802";

    public static String UNKNOWN_ASSET = "UNKNOWN";

    public static final String ASSET_COUNT = "tagCount";
    public static final String ID = "ID";
    public static final String KIT_ID = "KIT_ID";
    public static final String KIT_EPC = "KIT_EPC";
    public static final String KIT_NAME = "KIT_NAME";

    public static final String ASSET_TYPE_ID = "assetTypeID";
    public static final String ASSET_TYPE_NAME = "assetTypeName";
    public static final String ASSET_NAME = "assetName";
    public static final String ASSET_ID = "assetId";
    public static final String ASSET_STATUS = "assetStatus";
    public static final String ASSET_TAG_ID = "assetTagId";

    public static final String MENU_ID_MAPPING = "MAP_ID";
    public static final String MENU_ID_INVENTORY = "INVENTORY_ID";
    public static final String MENU_ID_SEARCH = "SEARCH_ID";
    public static final String MENU_ID_CHECKIN = "CHECKIN_ID";
    public static final String MENU_ID_CHECKOUT = "CHECKOUT_ID";
    public static final String MENU_ID_ASSETSYNC = "ASSETSYNC_ID";
    public static final String MENU_ID_ROOMCHECKOUT = "ROOMCHECKOUT_ID";
    public static final String MENU_ID_TRACKPOINT = "TRACKPOINT_ID";
    public static final String MENU_ID_SECURITYOUT = "SECURITYOUT_ID";





    public static List<LocationMaster> getLocationMasterList(){
        List<LocationMaster> list = new ArrayList<>();

        //Company Code = "21";
        //ID - 01 - SHIRT
        //ID - 02 - PANT
        //ID - 03 - JACKET
        //ID - 04 - TOWEL
        //ID - 05 - BEDSHIT
        //ID - 06 - PILLOW COVER

        String companycode = "15";//E2,21
        String shirt1 = "01";
        String pant2 = "02";
        String jacket3 = "03";
        String towel4 = "04";
        String bedshit5 = "05";
        String pilowcover6 = "06";

        LocationMaster a1 = new LocationMaster();
        a1.setLocationId("01");
        a1.setLocationName("Location 1");

        LocationMaster a2 = new LocationMaster();
        a2.setLocationId("02");
        a2.setLocationName("Location 2");

        LocationMaster a3 = new LocationMaster();
        a3.setLocationId("03");
        a3.setLocationName("Location 3");

        LocationMaster a4 = new LocationMaster();
        a4.setLocationId("04");
        a4.setLocationName("Location 4");

        LocationMaster a5 = new LocationMaster();
        a5.setLocationId("05");
        a5.setLocationName("Location 5");

        LocationMaster a6 = new LocationMaster();
        a6.setLocationId("06");
        a6.setLocationName("Location 6");

        list.add(a1);
        list.add(a2);
        list.add(a3);
        list.add(a4);
        list.add(a5);
        list.add(a6);
        return list;
    }

    public static final String DASHBOARD_MENU_MAPPING = "Mapping";
    public static final String DASHBOARD_MENU_INVENTORY = "Inventory";
    public static final String DASHBOARD_MENU_SEARCH = "Search";
    public static final String DASHBOARD_MENU_CHECKIN = "Check In";
    public static final String DASHBOARD_MENU_CHECKOUT = "Check Out";
    public static final String DASHBOARD_MENU_ROOMCHECKOUT = "Room Check Out";
    public static final String DASHBOARD_MENU_SECURITYOUT = "Security Out";
    public static final String DASHBOARD_MENU_TRACK_POINT = "Track Point";
    public static final String DASHBOARD_MENU_SYNC = "Sync";

}
