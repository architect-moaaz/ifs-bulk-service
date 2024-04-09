package io.intelliflow.services.helper;

 /*
    @author rahul
    @created on 20-10-2022
 */

import com.ctc.wstx.shaded.msv_core.verifier.jaxp.DocumentBuilderFactoryImpl;
import io.intelliflow.services.centralcustomexceptionhandler.CustomException;
import io.intelliflow.services.centralcustomexceptionhandler.StatusException;
import io.intelliflow.services.client.ExtensionService;
import io.intelliflow.services.models.RequestModel;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.*;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.client.exception.ResteasyWebApplicationException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

@ApplicationScoped
public class TemplateFileHelper {

    @Inject
    @RestClient
    ExtensionService extensionService;

    public final Map<String, List<String>> getDataModel(RequestModel requestModel) throws ParserConfigurationException, IOException, SAXException, CustomException {
        String xml;
        try {
            xml = extensionService.getMetaData(requestModel.getWorkspaceName(), requestModel.getMiniAppName());
        } catch (ResteasyWebApplicationException e) {
            if (e.getResponse().getStatus() == 404) {
                throw new CustomException("There is no data model present for the combination workspace: " + requestModel.getWorkspaceName() + " & mini app: " + requestModel.getMiniAppName(), StatusException.NOT_FOUND);
            }
            throw new CustomException(e.getMessage(), StatusException.INTERNAL_SERVER_ERROR);
        }
        Map<String, List<String>> keyToValue = new LinkedHashMap<>();
        DocumentBuilderFactory documentBuilderFactory = new DocumentBuilderFactoryImpl();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        var document = documentBuilder.parse(new InputSource(new StringReader(xml)));
        document.getDocumentElement().normalize();
        NodeList nodeList = document.getElementsByTagName("EntityType");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String collectionNameFromXML = element.getAttribute("Name");
                List<String> list = new ArrayList<>();
                NodeList nodeList1 = element.getChildNodes();
                for (int j = 0; j < nodeList1.getLength(); j++) {
                    Node node1 = nodeList1.item(j);
                    if (node1.getNodeType() == Node.ELEMENT_NODE) {
                        Element element1 = (Element) node1;
                        if (element1.getTagName().equalsIgnoreCase("Property")) {
                            list.add(element1.getAttribute("Name"));
                        }
                    }
                }
                if (requestModel.getDataModelName() != null && !requestModel.getDataModelName().isEmpty()) {
                    if (requestModel.getDataModelName().equalsIgnoreCase(collectionNameFromXML)) {
                        keyToValue.put(collectionNameFromXML, list);
                    }
                } else {
                    keyToValue.put(collectionNameFromXML, list);
                }
            }
        }
        return keyToValue;
    }

    public final byte[] createTemplateFile(RequestModel requestModel) throws IOException, ParserConfigurationException, SAXException, CustomException {
        Map<String, List<String>> excelFileInfo = getDataModel(requestModel);
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFCellStyle headerStyle = workbook.createCellStyle();
            XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            for (Map.Entry<String, List<String>> map : excelFileInfo.entrySet()) {
                List<String> itr = map.getValue();
                XSSFSheet spreadsheet = workbook.createSheet(map.getKey());
                XSSFRow row = spreadsheet.createRow(0);
                int cellId = 0;
                for (String str : itr) {
                    if (str.equalsIgnoreCase("_id"))
                        continue;
                    Cell cell = row.createCell(cellId++);
                    String header = str.substring(0, 1).toUpperCase() + str.substring(1);
                    cell.setCellValue(header);
                    cell.setCellStyle(headerStyle);
                    spreadsheet.setColumnWidth(cellId - 1, 4000); // Increase cell width to 4000 units
                }
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        }
    }
}
