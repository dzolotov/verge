/**
 This file is a part of Verge project.

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.

 @Author Dmitrii Zolotov <zolotov@gathe.org>, Tikhon Tagunov <tagunov@gathe.org>
 */

package org.gathe.verge.uno;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.container.XNameAccess;
import com.sun.star.datatransfer.XTransferable;
import com.sun.star.datatransfer.XTransferableSupplier;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XController;
import com.sun.star.frame.XStorable;
import com.sun.star.graphic.XGraphicObject;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.table.XCell;
import com.sun.star.table.XCellRange;
import com.sun.star.text.*;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.view.XSelectionSupplier;

import java.io.*;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.DatatypeConverter;
import org.json.simple.parser.*;
import org.json.simple.*;
import redis.clients.jedis.Jedis;

public class ODT2PDFConverter implements Runnable {

    private Jedis jedis;

    public ODT2PDFConverter(String templateName, String parameters, String sessionID, String callbackURL) {
        this.setTemplateName(templateName);
        this.setParameters(parameters);
        this.setSessionID(sessionID);
        this.setCallbackURL(callbackURL);
        this.jedis = new Jedis("localhost");
    }
    private String templateName;

    /**
     * Get the value of templateName
     *
     * @return the value of templateName
     */
    public String getTemplateName() {
        return templateName;
    }

    /**
     * Set the value of templateName
     *
     * @param templateName new value of templateName
     */
    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }
    private String parameters;

    /**
     * Get the value of parameters
     *
     * @return the value of parameters
     */
    public String getParameters() {
        return parameters;
    }

    /**
     * Set the value of parameters
     *
     * @param parameters new value of parameters
     */
    public void setParameters(String parameters) {
        this.parameters = parameters;
    }
    private String sessionID;

    /**
     * Get the value of sessionID
     *
     * @return the value of sessionID
     */
    public String getSessionID() {
        return sessionID;
    }

    /**
     * Set the value of sessionID
     *
     * @param sessionID new value of sessionID
     */
    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }
    private String callbackURL;

    /**
     * Get the value of callbackURL
     *
     * @return the value of callbackURL
     */
    public String getCallbackURL() {
        return callbackURL;
    }

    /**
     * Set the value of callbackURL
     *
     * @param callbackURL new value of callbackURL
     */
    public void setCallbackURL(String callbackURL) {
        this.callbackURL = callbackURL;
    }

    public void run() {
    
	System.setProperty("javax.net.ssl.trustStore","/opt/myKeystore");
	System.setProperty("javax.net.ssl.trustStorePassword","keys$%");
    
        FileInputStream is = null;
        try {
            XComponentContext xComponentContext = Bootstrap.bootstrap();
//	    XComponentContext xComponentContext = BootstrapSocketConnector.bootstrap(oooExeFolder);
            XMultiComponentFactory xMCF = xComponentContext.getServiceManager();

            HashMap<String, ArrayList<HashMap<String, String>>> data = new HashMap<>();
            HashMap<String, String> fields = new HashMap<>();

            String json = this.getParameters(); //"{\"lastname\":\"Иванов\",\"firstname\":\"Петр\",\"middlename\":\"Сидорович\",\"table-ege\":[{\"subject\":\"Physics\",\"Score\":\"100\"},{\"subject\":\"Chemistry\",\"Score\":\"95\"}]}";

            JSONParser parser = new JSONParser();
            System.out.println(json);
            JSONObject jsonObj = (JSONObject) parser.parse(json);
            for (Object jsonKey : jsonObj.keySet()) {
                if (("" + jsonKey).startsWith("table-")) {
                    ArrayList<HashMap<String, String>> tableContent = new ArrayList<>();
                    String tableName = ("" + jsonKey).substring(6);
                    JSONArray arr = (JSONArray) jsonObj.get(jsonKey);
                    HashMap<String, String> row;
                    for (int c = 0; c < arr.size(); c++) {
                        row = new HashMap<>();
                        JSONObject jobj = (JSONObject) arr.get(c);
                        for (Object rowKey : jobj.keySet()) {
                            String rowKeyStr = "" + rowKey;
                            String rowValue = "" + jobj.get(rowKey);
                            row.put(rowKeyStr.toUpperCase(), rowValue);
                        }
                        tableContent.add(row);
                    }
                    data.put(tableName.toUpperCase(), tableContent);
                } else {
                    String jsonValue = "" + jsonObj.get(jsonKey);
                    fields.put(("" + jsonKey).toUpperCase(), jsonValue);
                    System.out.println(jsonKey + ":" + jsonValue);
                }
            }


            //load file
            Object oDesktop = xMCF.createInstanceWithContext("com.sun.star.frame.Desktop", xComponentContext);
            XComponentLoader xCompLoader = (XComponentLoader) UnoRuntime.queryInterface(XComponentLoader.class, oDesktop);
            String url = this.getTemplateName();//"file:///devel/LOConnector/declaration_bachelor.odt";

            PropertyValue[] loadProperties = new PropertyValue[1];
            //        loadProperties[0] = new PropertyValue();
            //        loadProperties[0].Name = "Hidden";
            //        loadProperties[0].Value = new Boolean(false);
            loadProperties[0] = new PropertyValue();
            loadProperties[0].Name = "ReadOnly";
            loadProperties[0].Value = new Boolean(false);
            Object document = xCompLoader.loadComponentFromURL(url, "_blank", 0, loadProperties);
            
            XTextDocument xTextDocument = UnoRuntime.queryInterface(XTextDocument.class, document);

            XTextFieldsSupplier fieldsSupplier = UnoRuntime.queryInterface(XTextFieldsSupplier.class, document);
            //Заполнение полей и значений по выбору из вариантов ответа
            XNameAccess xNameAccess = fieldsSupplier.getTextFieldMasters();
            for (String element : fields.keySet()) {
                if (element.startsWith("#")) continue;
                System.out.println(element);
                String value = fields.get(element);
                System.out.println(value);
                if (value != null) {
                    String name = "com.sun.star.text.FieldMaster.User." + element.toUpperCase();
                    if (xNameAccess.hasByName(name)) {
                        Object fieldObj = xNameAccess.getByName(name);
                        XPropertySet field = UnoRuntime.queryInterface(XPropertySet.class, fieldObj);
                        String oldValue = field.getPropertyValue("Content").toString();
                        System.out.println(oldValue);
                        if (oldValue.startsWith("#")) {
                            if (value.equalsIgnoreCase("true")) {
                                value = "1";
                            }
                            if (value.equalsIgnoreCase("false")) {
                                value = "0";
                            }
                            String[] values = oldValue.substring(1).split("/");
                            boolean flag = false;
                            for (String value2 : values) {
                                String[] entry = value2.split("=");
                                if (entry.length==1) {
                                    String[] entry2 = new String[2];
                                    entry2[0] = entry[0];
                                    entry2[1] = "";
                                    entry = entry2;
                                }
                                System.out.println("Comparing "+entry[0]+" : "+entry[1]+" with "+value);
                                if (Integer.parseInt(entry[0]) == Integer.parseInt(value)) {
                                    flag = true;
                                    value = entry[1];
                                    break;
                                }
                            }
                            if (!flag) value = "<Неизвестно>";
                        }
                        field.setPropertyValue("Content", value);
                    }
                }
            }

            XTextTablesSupplier tablesSupplier = UnoRuntime.queryInterface(XTextTablesSupplier.class, document);

            //fill the tables
            XNameAccess allTables = tablesSupplier.getTextTables();
            for (String tableName : data.keySet()) {
                if (!allTables.hasByName(tableName)) {
                    continue;
                }
                Object retrievedTableObj = allTables.getByName(tableName);
                ArrayList<HashMap<String, String>> dataSubset = data.get(tableName);
                
                if (dataSubset.size()==0) {
                //remove empty table
	            XTextDocument textDocument = UnoRuntime.queryInterface(XTextDocument.class, document);
	            XText text = textDocument.getText();
                    XTextContent tableContentText = UnoRuntime.queryInterface(XTextContent.class, retrievedTableObj);
	            text.removeTextContent(tableContentText);
	    	    continue;
	    	}
                XTextTable retrievedTable = UnoRuntime.queryInterface(XTextTable.class, retrievedTableObj);
                XCellRange retrievedTableCells = UnoRuntime.queryInterface(XCellRange.class, retrievedTableObj);
                int rowsCount = retrievedTable.getRows().getCount();
                int colsCount = retrievedTable.getColumns().getCount();
                
                ArrayList<String> rowNumbers = new ArrayList<String>();
                ArrayList<HashMap<String,String>> fieldsLocations = new ArrayList<HashMap<String,String>>();
                ArrayList<String> rowLength = new ArrayList<String>();
                
                //get row numbers with referenced fields
                int maxRow = 0;
                int maxLen = 0;
        	for (int row = 0; row<rowsCount; row++) {
        	    boolean found = false;
        	    HashMap<String,String> fieldsAtRow = new HashMap<String,String>();
        	    int col=0;
        	    for (col=0; col<colsCount; col++) {
        		System.out.println("Reading cell "+col+":"+row);
        		try {
	        		XCell cell = retrievedTableCells.getCellByPosition(col, row);
    		        	XText cellText = UnoRuntime.queryInterface(XText.class, cell);
            			String val = cellText.getString();
	                	if (val.startsWith("#")) {
    		        	    fieldsAtRow.put(""+col,val.substring(1));
            			    found = true;
	                	}
	                } catch (Exception e) {
	            	    break;
	                }
                    }
                    if (found) {
                	rowNumbers.add(""+row);
                	fieldsLocations.add(fieldsAtRow);
                	rowLength.add(""+col);
                        if (row>maxRow) maxRow = row;
                        if (col>maxLen) maxLen = col;
            	    }
                }
                maxRow++;
                
                XController xController = xTextDocument.getCurrentController();
                XTransferableSupplier xTransferableSupplier = UnoRuntime.queryInterface(XTransferableSupplier.class, xController);
                
                //Fill the table below the bottom line
                int tableRow = maxRow;
                for (HashMap<String, String> dataRow : dataSubset) {
//                    if (tableRow != 0) {
		    System.out.println("Adding row "+maxRow);
                    retrievedTable.getRows().insertByIndex(maxRow, 1);
                    maxRow++;
                }
                
                for (HashMap<String,String> dataRow : dataSubset) {
                    //merging cells
            	    System.out.println("Datarow: "+dataRow);
                    int rowId = 0;
                    if (dataRow.containsKey("%")) rowId = Integer.parseInt(""+dataRow.get("%"));
                    int rowLen = Integer.parseInt(rowLength.get(rowId));
                    if (dataRow.containsKey("$")) {
                      System.out.println("MERGING CELLS");
                      int left = maxLen;
                      int positionX = 0;
                      int positionY = tableRow;
                      String[] dataRowSize = dataRow.get("$").split(",");
                      int newLen = 0;
                      for (String dataRowSizeElement : dataRowSize) {
                        System.out.println("PROCESSING: "+dataRowSizeElement);
                        int horSpan;
                        if (dataRowSizeElement.equals("*")) horSpan = left; else horSpan=Integer.parseInt(dataRowSizeElement);
                        String startAddress = String.valueOf(Character.toChars(positionX+65))+(positionY+1);
			XTextTableCursor textTableCursor = retrievedTable.createCursorByCellName(startAddress);
			if (horSpan>1) textTableCursor.goRight((short)(horSpan-1),true);
			textTableCursor.mergeRange();
                        left-=horSpan;
                        newLen++;
                        if (left<=0) break;
                      }
                      rowLen = newLen;
                    }
                    
                    System.out.println("Extracting range for 0:"+rowNumbers.get(rowId)+":"+(rowLen-1)+":"+rowNumbers.get(rowId));
                    XCellRange templateRow = retrievedTableCells.getCellRangeByPosition(0, Integer.parseInt(rowNumbers.get(rowId)), rowLen-1, Integer.parseInt(rowNumbers.get(rowId)));
                    XSelectionSupplier xSelectionSupplier = UnoRuntime.queryInterface(XSelectionSupplier.class, xController);
                    xSelectionSupplier.select(templateRow);
                    XTransferable transferable = xTransferableSupplier.getTransferable();
            	    System.out.println("Getting cellRange 0:"+tableRow+":"+(rowLen-1)+":"+tableRow);
                    XCellRange newRow = retrievedTableCells.getCellRangeByPosition(0, tableRow, rowLen-1, tableRow);
                    xSelectionSupplier.select(newRow);
                    xTransferableSupplier.insertTransferable(transferable);
//                    }
                    for (int tableCol = 0; tableCol < rowLen; tableCol++) {
                    	
                    	HashMap<String,String> rowMetadata = fieldsLocations.get(rowId);
                    		if (rowMetadata.containsKey(""+tableCol)) {
                    			String fieldId = rowMetadata.get(""+tableCol);
                    			String dataItem = dataRow.get(fieldId);
                    			System.out.println("Retrieving cell:"+tableCol+":"+tableRow);
                    			XCell cell = retrievedTableCells.getCellByPosition(tableCol, tableRow);
                    			System.out.println("Setting to cell "+tableCol+":"+tableRow+" value: "+dataItem);
                    			XText cellText = UnoRuntime.queryInterface(XText.class, cell);
                    			cellText.setString(dataItem);
                    		}
                    }
//                    if (tableRow != 0) {
                        tableRow++;
//                    }
                }
                for (int i=rowNumbers.size()-1;i>=0;i--) {
                    retrievedTable.getRows().removeByIndex(Integer.parseInt(rowNumbers.get(i)),1);
                }
            }

            //show/hide frames
            XTextFramesSupplier framesSupplier = UnoRuntime.queryInterface(XTextFramesSupplier.class, document);
            XNameAccess frames = framesSupplier.getTextFrames();
            XTextDocument textDocument = UnoRuntime.queryInterface(XTextDocument.class, document);
            XText text = textDocument.getText();
            for (String fieldName : fields.keySet()) {
                if (!fieldName.startsWith("#")) {
                    continue;
                }
                String fieldName2 = fieldName.substring(1);
                String value = fields.get(fieldName);
                if (frames.hasByName(fieldName2)) {
                    System.out.println("Checking frame "+fieldName2);
                    if (value.trim().equalsIgnoreCase("0")) {
                        System.out.println("Hiding");
                        Object frameContent = frames.getByName(fieldName2);
                        XTextContent frameContentText = UnoRuntime.queryInterface(XTextContent.class, frameContent);
                        text.removeTextContent(frameContentText);
                    }
                }
            }

            //update graphic objects
            XTextGraphicObjectsSupplier goSupplier = UnoRuntime.queryInterface(XTextGraphicObjectsSupplier.class, document);
            XNameAccess graphicsObjects = goSupplier.getGraphicObjects();
            for (String objName : fields.keySet()) {
                if (!objName.startsWith("@")) continue;
                String value = fields.get(objName);         //encoded binary image
                byte[] imageData = DatatypeConverter.parseBase64Binary(value);
                File temp = File.createTempFile("image", ".jpg",new File("/tmp"));
                FileOutputStream fos = null;
                fos = new FileOutputStream(temp);
                fos.write(imageData);
                fos.flush();
                fos.close();
                String newURL = temp.getCanonicalPath();
                String goName = objName.substring(1);
                if (graphicsObjects.hasByName(goName)) {
                    XPropertySet graphicObject = UnoRuntime.queryInterface(XPropertySet.class,graphicsObjects.getByName(goName));
                    graphicObject.setPropertyValue("GraphicURL", "file://"+newURL);
                }
            }
            
            //transform file

            //save file
            String sSaveURL = "file:///tmp/" + this.getSessionID() + ".pdf";

 
            XStorable xStorable = (XStorable) UnoRuntime.queryInterface(XStorable.class, document);
            PropertyValue[] propertyValue = new PropertyValue[2];
            propertyValue[0] = new PropertyValue();
            propertyValue[0].Name = "Overwrite";
            propertyValue[0].Value = new Boolean(true);
            propertyValue[1] = new PropertyValue();
            propertyValue[1].Name = "FilterName";
            propertyValue[1].Value = "writer_pdf_Export";

            xStorable.storeToURL(sSaveURL, propertyValue);
            XComponent component = (XComponent) UnoRuntime.queryInterface(XComponent.class, document);
            component.dispose();

            byte[] key = this.getSessionID().getBytes("utf-8");
            File file = new File("/tmp/" + this.getSessionID() + ".pdf");
            is = new FileInputStream(file);
            long length = file.length();

            byte[] bytes = new byte[(int) length];

            // Read in the bytes
            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }
            System.out.println("Here");
            jedis.set(key, bytes);
            jedis.expire(key,30*60);		//30 minutes to expire
            is.close();
            file.delete();

//            String url2 = this.getCallbackURL();
//            if (url2.trim().length()!=0) {
//              url2 = url2.replace("~SESSION~", this.getSessionID());
//              URL urlx;
//              try {
//                  urlx = new URL(url2);
//                  urlx.openStream();
//              } catch (IOException ex) {
//                  Logger.getLogger(ODT2PDFConverter.class.getName()).log(Level.SEVERE, null, ex);
//              }
//            }
            //        XCloseable xCloseable = (XCloseable) UnoRuntime.queryInterface(XCloseable.class, document);
            //        if (xCloseable!=null) {
            //            xCloseable.close(false);
            //        }

        } catch (IOException | Exception | ParseException | BootstrapException ex) {
            Logger.getLogger(ODT2PDFConverter.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(ODT2PDFConverter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
