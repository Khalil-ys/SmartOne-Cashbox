package az.smartonecashbox.smartCassa;



import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.cache.spi.support.AbstractReadWriteAccess;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;



public class Hasher {
    private final static String merchantId="merchantId";
    private final static String cassaIp = "cassaIp";
    private final static String employeeName = "employeeName";
    private final static String pincode = "pincode";
    private static Gson gson = new Gson();

    public static void main(String[] args) throws NoSuchAlgorithmException, IOException, JSONException {
        ConnectionDB connectionDB = new ConnectionDB();
        Connection connection = connectionDB.getConnection();
        System.out.println(openShift(connection));
    }

    public static String openShift(Connection connection) throws NoSuchAlgorithmException, IOException {
        OpenShiftRequest request = new OpenShiftRequest();
        Map<String, String> cassaData = getCassaData(connection);
        request.setEmployeeName(cassaData.get(employeeName));
        request.setPinCode(cassaData.get(pincode));
        String resp = getData(connection, request, "open_shift");
        System.out.println(resp);
        try {
            JSONObject jsonObject = new JSONObject(resp);
            String status = jsonObject.getString("status");
            if(status.equals("success")){
                String shiftId=jsonObject.getString("shiftID");

                insertNewShif(connection, shiftId);
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return resp;
    }

    public static String closeShift(Connection connection) throws NoSuchAlgorithmException, IOException {
        CloseShiftRequest request = new CloseShiftRequest();
        Map<String, String> cassaData = getCassaData(connection);
        request.setEmployeeName(cassaData.get(employeeName));
        String resp= getData(connection, request, "close_shift");
        updateShift(connection);
        return resp;
    }

    public static String checkShift(Connection connection) throws NoSuchAlgorithmException, IOException {
        CheckShiftRequest request = new CheckShiftRequest();
        Map<String, String> cassaData = getCassaData(connection);
        request.setEmployeeName(cassaData.get(employeeName));
        return getData(connection, request, "check_shift");
    }

    public static String sale(Connection connection, String id, String cashType) throws NoSuchAlgorithmException, IOException {
        System.out.println(new JSONObject(getSale(connection, id, cashType)));
        return getData(connection, getSale(connection, id, cashType), "sale");
    }

    public static String refund(Connection connection, String invoice_id, String sell_id, String cashType, String fiscalId) throws NoSuchAlgorithmException, IOException {
        Sale refundRequest = getReturnData(connection, invoice_id, cashType, fiscalId);
        refundRequest.setDocNumber(sell_id);
        return getData(connection, refundRequest, "refund");
    }


    public static <T> String getData(Connection connection, T entity, String method) throws NoSuchAlgorithmException, IOException {
        String json = gson.toJson(entity);
        String encodeBytes = Base64.getEncoder().encodeToString(json.getBytes());
        Map<String, String> cassaData = getCassaData(connection);
        String sha1=stringToHexString(encodeBytes+cassaData.get(merchantId));
        String sign = Base64.getEncoder().encodeToString(sha1.getBytes());
        String data="data="+encodeBytes.replaceAll("=", "%3D")+"&sign="+sign.replaceAll("=",  "%3D");
        System.out.println("geldi getDataya  :   "+data);
        String resp =ProxyClient.callProxy(data, cassaData.get(cassaIp)+method);
        System.out.println(resp);
        saveLog(connection, method, json, data, resp);
        return resp;
    }

    public static String stringToHexString(String data) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        ByteArrayInputStream fis = new ByteArrayInputStream(data.getBytes());

        byte[] dataBytes = new byte[1024];

        int nread = 0;
        while ((nread = fis.read(dataBytes)) != -1) {
            md.update(dataBytes, 0, nread);
        };
        byte[] mdbytes = md.digest();

        //convert the byte to hex format method 1
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < mdbytes.length; i++) {
            sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
        }

        System.out.println("Hex format : " + sb.toString());

        //convert the byte to hex format method 2
        StringBuffer hexString = new StringBuffer();
        for (int i=0;i<mdbytes.length;i++) {
            hexString.append(Integer.toHexString(0xFF & mdbytes[i]));
        }

        System.out.println("Hex format : " + hexString.toString());
        return sb.toString();
    }

    public static Sale getSale(Connection connection, String id, String cashType) {
        PreparedStatement pstmt=null;
        ResultSet rs=null;
        ConnectionDB db=new ConnectionDB();
        List<Item> details = new ArrayList<>();
        String docTime = "";

        Map<String, String> cassaData = getCassaData(connection);
        String employeeName1 = cassaData.get(employeeName);
        String currency="AZN";
        String c_type = "";
        String sql="SELECT "+
                "     P.ID AS P_ID, g.CASH_TYPE as CASH_TYPE,  "+
                "     P.NAME,"+
                "     ROUND (SUM (GD.SUM_AMOUNT), 2) * 100 AS SUM_AMOUNT,"+
                "     ROUND (SUM (GD.COUNT) / (GD.P_COUNT), 2)*1000 AS REAL_COUNT,"+
                "     GD.PRICE, GD.EXPIRE_DATE AS EXPIRE_DATE, "+
                "     P_TYPE.NAME AS T_NAME, g.FISCAL_ID,"+
                "     S.NAME AS S_NAME, to_char(G.SELL_DATE,'yyyy-MM-dd HH24:MI:SS') SELL_DATE"+
                "   FROM GOO_SELL G"+
                "     INNER JOIN GOO_SELL_DETAIL GD"+
                "        ON G.ID = GD.SELL_ID AND GD.DELETED = 0 "+
                "     INNER JOIN PRE_PREPARAT P"+
                "        ON GD.PREPARAT_ID = P.ID"+
                "     INNER JOIN ATR_ATRIBUT P_TYPE"+
                "        ON P.PREPARAT_TYPE_ID = P_TYPE.ID"+
                "     INNER JOIN ATR_ATRIBUT S"+
                "        ON P.STRUCTURE_ID = S.ID AND"+
                "    G.ID="+id+
                "  GROUP BY "+
                "     P.ID,"+
                "     P.NAME,"+
                "     GD.PRICE,"+
                "     P_TYPE.NAME, GD.P_COUNT,  G.SELL_DATE,  "+
                "     S.NAME , GD.EXPIRE_DATE,g.CASH_TYPE, g.FISCAL_ID "+
                "  ORDER BY P.NAME ASC";
        int sumAmount = 0;
        String fiscalId="";
        System.out.println(sql);
        try{
            pstmt=connection.prepareStatement(sql);
            rs=pstmt.executeQuery();
            while(rs.next()){

                Item item = new Item();
                item.setItemId(rs.getString("P_ID"));
                item.setItemName(rs.getString("name"));
                item.setDiscount(0);
                item.setItemAttr(0);
                item.setItemQRCode(" ");
                item.setItemAmount(rs.getInt("SUM_AMOUNT"));
                item.setItemQty(rs.getInt("REAL_COUNT"));
                item.setItemTaxes(Arrays.asList(new ItemTaxe()));
                sumAmount = sumAmount+rs.getInt("SUM_AMOUNT");
                details.add(item);
                docTime = rs.getString("sell_date");
                fiscalId= rs.getString("FISCAL_ID");
                c_type = rs.getString("CASH_TYPE");
            }
        }catch(SQLException ex){
            ex.printStackTrace();
        }finally{
            db.closeResultSet(rs);
            db.closePreparedStatement(pstmt);
        }
        Sale sale = new Sale();
        sale.setAmount(sumAmount);
        sale.setCurrency(currency);
        sale.setDocNumber(id);
        sale.setDepartmentCode("");
        sale.setDepartmentName("Zeytun 403");
        sale.setEmployeeName(employeeName1);
        sale.setItems(details);
        sale.setDocTime(docTime);
        Payment payment = new Payment();
        System.out.println("c_type   :   "+c_type);
        System.out.println("cashType   :   "+cashType);
        if(c_type!=null && !c_type.trim().equals("")) {
            cashType = c_type;
        }
        if(cashType.equals("1")) {
            payment.setCashAmount(sumAmount);
        }else {
            payment.setCashlessAmount(sumAmount);
        }

        System.out.println("cashType   :   "+cashType);
        if(fiscalId!=null && !fiscalId.trim().equals("")) {
            sale.setFiscalID(fiscalId);
        }else {
            sale.setFiscalID("");
        }
        sale.setPayments(payment);
        sale.setWsName("1");
        sale.setCreditContract("");
        sale.setPrepayDocID("");
        sale.setPrepayDocNum("");
        sale.setPrintFooter("");


        updateGooSellForCashType(connection, id, cashType);

        return sale;
    }


    public static void updateGooSellForFiscal(Connection connection, String id, String fiscalId) {
        PreparedStatement pstmt=null;
        ConnectionDB db=new ConnectionDB();
        String sql = "update goo_sell set fiscal_id='"+fiscalId+"' where id="+id;

        try{
            pstmt=connection.prepareStatement(sql);
            pstmt.executeUpdate();
        }catch(SQLException ex){
            ex.printStackTrace();
        }finally{
            db.closePreparedStatement(pstmt);
        }
    }

    public static void updateGooSellForCashType(Connection connection, String id, String cashType) {
        PreparedStatement pstmt=null;
        ConnectionDB db=new ConnectionDB();
        String sql = "update goo_sell set CASH_TYPE='"+cashType+"' where id="+id;

        try{
            pstmt=connection.prepareStatement(sql);
            pstmt.executeUpdate();
        }catch(SQLException ex){
            ex.printStackTrace();
        }finally{
            db.closePreparedStatement(pstmt);
        }
    }

    public static void insertNewShif(Connection connection, String shiftId) {
        PreparedStatement pstmt=null;
        ConnectionDB db=new ConnectionDB();
        String sql = "Insert into CASSA_SHIFT (SHIFT_ID, ACTIVE, CREATED_DATE) Values " +
                "('"+shiftId+"', 0, sysdate)";

        try{
            pstmt=connection.prepareStatement(sql);
            pstmt.executeUpdate();
        }catch(SQLException ex){
            ex.printStackTrace();
        }finally{
            db.closePreparedStatement(pstmt);
        }
    }

    public static void updateShift(Connection connection) {
        PreparedStatement pstmt=null;
        ConnectionDB db=new ConnectionDB();
        String sql = "Update CASSA_SHIFT set active=1 , update_at=sysdate where active=0 ";

        try{
            pstmt=connection.prepareStatement(sql);
            pstmt.executeUpdate();
        }catch(SQLException ex){
            ex.printStackTrace();
        }finally{
            db.closePreparedStatement(pstmt);
        }
    }



    public static String getShiftId(Connection connection) {
        PreparedStatement pstmt=null;
        ResultSet rs=null;
        ConnectionDB db=new ConnectionDB();
        String sellSql = "select SHIFT_ID from CASSA_SHIFT where active=0";
        String shiftId="";
        try{
            pstmt=connection.prepareStatement(sellSql);
            rs=pstmt.executeQuery();
            while(rs.next()){
                shiftId = rs.getString("SHIFT_ID");
            }}catch(SQLException ex){
            ex.printStackTrace();
        }finally{
            db.closeResultSet(rs);
            db.closePreparedStatement(pstmt);
        }
        return shiftId;
    }



    public static void updateGooSellRForSellId(Connection connection, String id, String sellId) {
        PreparedStatement pstmt=null;
        ConnectionDB db=new ConnectionDB();
        String sql = "update goo_sell_r set SELL_ID='"+sellId+"' where id="+id;

        try{
            pstmt=connection.prepareStatement(sql);
            pstmt.executeUpdate();
        }catch(SQLException ex){
            ex.printStackTrace();
        }finally{
            db.closePreparedStatement(pstmt);
        }
    }

    public static String getSaleStatusCode(int code) {
        List<String> errorCodes = new ArrayList<String>();
        errorCodes.add("Xəta yoxdur");
        errorCodes.add("Avtorizasiyada xəta ");
        errorCodes.add("JSON-un yalnış formatı");
        errorCodes.add("Mütləq parametrlər göndərilməyib");
        errorCodes.add("Sənəd üzrə məbləğ mövqelər üzrə məbləğə uyğun deyil");
        errorCodes.add("Daxili xəta");
        errorCodes.add("Növbə açılmayıb");
        errorCodes.add("Ödəniş növü dəstəklənmir ");
        errorCodes.add("Sənəd tam ödənilməyib (avtomatik fiskalizasiya zamanı)");
        errorCodes.add("Sənəd tapılmadı");
        return errorCodes.get(code);
    }


    private static void saveLog(Connection connection, String method, String json, String base64, String response) {
        PreparedStatement pstmt=null;
        ConnectionDB db=new ConnectionDB();
        String docId = "";
        String fiscalId="";
        if(method.equals("sale")) {
            JSONObject object;
            JSONObject object1;
            try {
                object = new JSONObject(json);
                docId = object.getString("docNumber");

            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                object1 = new JSONObject(response);
                fiscalId = object1.getString("fiscalID");
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if(docId!="" && fiscalId!="") {
                updateGooSellForFiscal(connection, docId, fiscalId);
            }

        }

        String sql="Insert into YUVENS_DB.CASSA_LOG " +
                "   (REQUEST_DATA, METHOD, BASE64DATA,  " +
                "    SYSTEM_DATE, RESPONSE_DATA, FISCAL_ID, SELL_ID) " +
                " Values " +
                "   ('"+json+"', '"+method+"', '"+base64+"', " +
                "    SYSDATE, '"+response+"', '"+fiscalId+"', '"+docId+"')";
        try{
            pstmt=connection.prepareStatement(sql);
            pstmt.executeUpdate();
        }catch(SQLException ex){
            ex.printStackTrace();
        }finally{
            db.closePreparedStatement(pstmt);
        }
    }

    public static Sale getReturnData(Connection connection, String id, String cashType, String fiscalId) {
        PreparedStatement pstmt=null;
        ResultSet rs=null;
        ConnectionDB db=new ConnectionDB();
        List<Item> details = new ArrayList<>();
        Map<String, String> cassaData = getCassaData(connection);
        String employeeName1 = cassaData.get(employeeName);
        String currency="AZN";
        String docTime = "";
        String sql="SELECT P.ID AS P_ID,\r\n" +
                "         P.NAME,\r\n" +
                "         round(SUM (GD.AMOUNT),2)*100 AS SUM_AMOUNT,\r\n" +
                "         round((SUM (GD.COUNT)/GD.P_COUNT),2)*1000 AS COUNT,\r\n" +
                "         P_TYPE.NAME AS T_NAME,\r\n" +
                "         S.NAME AS S_NAME, to_char(G.RETURN_DATE,'yyyy-MM-dd HH24:MI:SS') RETURN_DATE,   \r\n" +
                "         st.expire_date\r\n" +
                "    FROM GOO_SELL_R G\r\n" +
                "         INNER JOIN GOO_SELL_RETURN GD\r\n" +
                "            ON G.ID = GD.RETURN_ID AND GD.DELETED = 0\r\n" +
                "         INNER JOIN PRE_PREPARAT P ON GD.PREPARAT_ID = P.ID\r\n" +
                "         INNER JOIN ATR_ATRIBUT P_TYPE ON P.PREPARAT_TYPE_ID = P_TYPE.ID\r\n" +
                "         INNER JOIN ATR_ATRIBUT S ON P.STRUCTURE_ID = S.ID\r\n" +
                "         INNER JOIN STO_STOCK_IN_PART_DETAIL ST\r\n" +
                "            ON ST.ID = GD.STOCK_IN_PART_DETAIL_ID AND G.ID = "+id +
                "GROUP BY P.ID,\r\n" +
                "         P.NAME,\r\n" +
                "         P_TYPE.NAME,\r\n" +
                "         S.NAME,G.RETURN_DATE,   GD.P_COUNT ,\r\n" +
                "         st.expire_date\r\n" +
                "ORDER BY P.NAME ASC";
        int sumAmount = 0;
        System.out.println(sql);
        try{
            pstmt=connection.prepareStatement(sql);
            rs=pstmt.executeQuery();
            while(rs.next()){

                Item item = new Item();
                item.setItemId(rs.getString("P_ID"));
                item.setItemName(rs.getString("name"));
                item.setDiscount(0);
                item.setItemAttr(0);
                item.setItemQRCode(" ");
                item.setItemAmount(rs.getInt("SUM_AMOUNT"));
                item.setItemQty(rs.getInt("COUNT"));
                item.setItemTaxes(Arrays.asList(new ItemTaxe()));
                sumAmount = sumAmount+rs.getInt("SUM_AMOUNT");
                docTime = rs.getString("RETURN_DATE");
                details.add(item);


            }
        }catch(SQLException ex){
            ex.printStackTrace();
        }finally{
            db.closeResultSet(rs);
            db.closePreparedStatement(pstmt);
        }
        Sale sale = new Sale();
        sale.setAmount(sumAmount);
        sale.setCurrency(currency);
        sale.setDocNumber(id);
        sale.setDepartmentCode("");
        sale.setDepartmentName("Zeytun 403");
        sale.setEmployeeName(employeeName1);
        sale.setItems(details);
        Payment payment = new Payment();
        if(cashType.equals("1")) {
            payment.setCashAmount(sumAmount);
        }else {
            payment.setCashlessAmount(sumAmount);
        }
        sale.setDocTime(docTime);
        sale.setPayments(payment);
        sale.setPrepayDocID(fiscalId);
        sale.setPrepayDocNum("");
        sale.setRrn("");
        sale.setCheckNum("");
        sale.setWsName("1");
        sale.setOriginAmount(sumAmount);
        sale.setCreditContract("");
        sale.setPrepayDocID("");
        sale.setPrepayDocNum("");
        sale.setPrintFooter("");
        return sale;
    }

    public static String getFiscalId(Connection connection, String docId) {
        PreparedStatement pstmt=null;
        ResultSet rs=null;
        ConnectionDB db=new ConnectionDB();
        String sellSql = "select fiscal_id from goo_sell where id="+docId;
        String fiscalId="";
        try{
            pstmt=connection.prepareStatement(sellSql);
            rs=pstmt.executeQuery();
            while(rs.next()){
                fiscalId = rs.getString("fiscal_id");
            }}catch(SQLException ex){
            ex.printStackTrace();
        }finally{
            db.closeResultSet(rs);
            db.closePreparedStatement(pstmt);
        }
        return fiscalId;
    }

    public static String getCahsType(Connection connection, String docId) {
        PreparedStatement pstmt=null;
        ResultSet rs=null;
        ConnectionDB db=new ConnectionDB();
        String sellSql = "select CASH_TYPE from goo_sell where id="+docId;
        String cashType="";
        try{
            pstmt=connection.prepareStatement(sellSql);
            rs=pstmt.executeQuery();
            while(rs.next()){
                cashType = rs.getString("CASH_TYPE");
            }}catch(SQLException ex){
            ex.printStackTrace();
        }finally{
            db.closeResultSet(rs);
            db.closePreparedStatement(pstmt);
        }
        return cashType;
    }

    public static Map<String, String> getCassaData(Connection connection) {
        PreparedStatement pstmt=null;
        ResultSet rs=null;
        ConnectionDB db=new ConnectionDB();
        String sql = "select * from cassa_data";
        System.out.println("cassa data sql  :  "+sql);
        Map<String, String> cassaData = new HashMap<String, String>();
        try{
            pstmt=connection.prepareStatement(sql);
            rs=pstmt.executeQuery();
            while(rs.next()){
                cassaData.put(rs.getString("key"), rs.getString("value"));
                System.out.println(rs.getString("key") +"  :   "+rs.getString("value"));
            }}catch(SQLException ex){
            ex.printStackTrace();
        }finally{
            db.closeResultSet(rs);
            db.closePreparedStatement(pstmt);
        }
        return cassaData;
    }

    public static String getXReport(Connection connection) throws NoSuchAlgorithmException, IOException {
        XReportRequest reportRequest = new XReportRequest();
        String siftId = getShiftId(connection);
        reportRequest.setShiftID(siftId);
        return getData(connection, reportRequest, "x_report");
    }

}
