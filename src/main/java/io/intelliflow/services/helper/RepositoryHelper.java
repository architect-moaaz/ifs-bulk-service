package io.intelliflow.services.helper;

import com.ctc.wstx.shaded.msv_core.verifier.jaxp.DocumentBuilderFactoryImpl;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import io.intelliflow.services.fileoperationsexceptions.FileOperationsException;
import io.intelliflow.services.models.*;
import io.intelliflow.services.repo.IdCountPerRequestRepository;
import io.intelliflow.services.repo.ProcessQueueRepository;
import io.intelliflow.services.repo.TemplateQueueRepository;
import io.quarkus.logging.Log;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
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
import java.io.*;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@ApplicationScoped
public class RepositoryHelper {

    @Inject
    MongoClient mongodbClient;

    @Inject
    ProcessQueueRepository processQueueRepository;

    @Inject
    TemplateQueueRepository templateQueueRepository;

    @ConfigProperty(name = "file.location")
    String fileLocation;

    @Inject
    IdCountPerRequestRepository idCountPerRequestRepository;


    public final List<Document> fileUpload(boolean onlyValidate, String xml, RequestModel requestModel) throws IOException, ParserConfigurationException, SAXException, FileOperationsException {
        InputStream inputStream = new ByteArrayInputStream(requestModel.getExcelContent());
        XSSFWorkbook xlBook = new XSSFWorkbook(inputStream);
        for (Sheet xSheet : xlBook) {
            requestModel.setSheetName(xSheet.getSheetName());
            Map<Integer, String> columnNames = new HashMap<>();
            int colNum = xSheet.getRow(0).getLastCellNum();
            List<String> data = new ArrayList<>();
            Map<Map<String, Boolean>, String> keyToValue = parseXML(xml, xSheet.getSheetName(), requestModel);
            if (xSheet.getRow(0).cellIterator().hasNext()) {
                for (int j = 0; j < colNum; j++) {
                    String propertyName = Character.toLowerCase(xSheet.getRow(0).getCell(j).toString().charAt(0))
                            + xSheet.getRow(0).getCell(j).toString().substring(1);
                    propertyName = propertyName.replaceAll("\\s", "");
                    columnNames.put(j, propertyName);
                    data.add(propertyName);
                }
            }
            List<String> mandatoryHeaders = new ArrayList<>();
            Map<String, String> stringStringMap = new HashMap<>();
            Map<String, Boolean> mandatoryColumns = new HashMap<>();
            for (Map.Entry<Map<String, Boolean>, String> entry : keyToValue.entrySet()) {
                for (Map.Entry<String, Boolean> entry1 : entry.getKey().entrySet()) {
                    if (Boolean.TRUE.equals(entry1.getValue())) {
                        mandatoryHeaders.add(entry1.getKey());
                    }
                    stringStringMap.put(entry1.getKey(), entry.getValue());
                    mandatoryColumns.put(entry1.getKey(), entry1.getValue());
                }
            }
            validateHeaders(data, mandatoryHeaders, requestModel);
            Iterator<Row> itr = xSheet.iterator();
            return validateDataAndCreateLog(itr, columnNames, stringStringMap, requestModel, mandatoryColumns,onlyValidate,xSheet);
        }
        return null;
    }

    public final void validateHeaders(List<String> data, List<String> keyToValue, RequestModel requestModel) throws FileOperationsException {
        StringBuilder errorInHeaders = new StringBuilder();
        ArrayList<Document> documentList = new ArrayList<>();
        Document document = new Document();
        for (String propertyName : keyToValue) {
            document.append(propertyName, "");
            Optional<String> keyResult = data.stream().filter(e -> e.equalsIgnoreCase(propertyName)).findAny();
            if (keyResult.isEmpty()) {
                if (errorInHeaders.length() > 0) {
                    errorInHeaders.append(" and ").append(propertyName).append(" is one of the mandatory columns, This is either missing or misspelled in the uploaded file.");
                } else {
                    errorInHeaders.append(propertyName).append(" is one of the mandatory columns, This is either missing or misspelled in the uploaded file.");
                }
            }
        }
        if (errorInHeaders.length() > 0) {
            document.append("Reason", errorInHeaders.toString());
            documentList.add(document);
            uploadErrorToDBAndLogStatus(documentList, requestModel,true,0,0);
        }
    }

    public final void uploadErrorToDBAndLogStatus(List<Document> documentList, RequestModel requestModel,boolean completeLyFailed,int uploadedRecords,int failedRecords) throws FileOperationsException {
        String collectionName = (requestModel.getWorkspaceName() + "-" + requestModel.getMiniAppName() + "-" + requestModel.getUploadId()).toLowerCase();
        var dataBaseErrorLogFiles = mongodbClient.getDatabase("errorLogs");
        dataBaseErrorLogFiles.getCollection(collectionName).insertMany(documentList);
        if(completeLyFailed) {
            throw new FileOperationsException(documentList, 500,uploadedRecords,failedRecords);
        }else {
            throw new FileOperationsException(documentList, 206,uploadedRecords,failedRecords);
        }
    }

    public final List<ProcessQueue> getStatus(RequestModel requestModel, int pageNumber, int pageSize) {
        if (requestModel == null) {
            return processQueueRepository.list(pageNumber,pageSize);
        } else {
            if (requestModel.getUploadId() == -1) {
                if (requestModel.getMiniAppName() == null) {
                   return processQueueRepository.listWithWorkSpace(requestModel.getWorkspaceName(),pageNumber,pageSize);
                } else {
                    return processQueueRepository.listWithAppNameAndWorkSpace(requestModel.getWorkspaceName(),requestModel.getMiniAppName(),pageNumber,pageSize);
                }
            } else {
                return processQueueRepository.listWithUploadIdAndWorkSpace(requestModel.getWorkspaceName(), requestModel.getUploadId(),pageNumber,pageSize);
            }
        }
    }

    public final RequestModel deleteTemplate(RequestModel requestModel) {
        try {
            boolean deleted = templateQueueRepository.deleteTemplateQueue(requestModel.getWorkspaceName(), requestModel.getUploadId()) == 1;
            requestModel.setDeleted(deleted);
            return requestModel;
        }
        catch (Exception e){
            requestModel.setDeleted(false);
            return requestModel;
        }
    }

    public final List<TemplateQueue> getTemplateStatus(RequestModel requestModel, int pageNumber, int pageSize) {
        return templateQueueRepository.listWithPagination(requestModel.getWorkspaceName(), pageNumber, pageSize);
    }

    public final byte[] createLogFileFromDB(RequestModel requestModel) {
        String collectionName = (requestModel.getWorkspaceName() + "-" + requestModel.getMiniAppName() + "-" + requestModel.getUploadId()).toLowerCase();
        var database = mongodbClient.getDatabase("errorLogs");
        MongoCursor<Document> documentIterable = database.getCollection(collectionName).find().iterator();
        Map<Integer, List<String>> data = new HashMap<>();
        int id = 0;
        while (documentIterable.hasNext()) {
            Document iterator = documentIterable.next();
            if (id == 0) {
                List<String> list = new ArrayList<>(iterator.keySet());
                list.remove(0);
                data.put(id++, list);
            }
            List<String> list = new ArrayList<>();
            Collection<Object> collection = iterator.values();
            boolean firstIndex = true;
            for (Object o : collection) {
                if(firstIndex){
                    firstIndex = false;
                    continue;
                }
                list.add(o.toString());
            }
            data.put(id++, list);
        }
        documentIterable.close();
        return createLogFile(data, requestModel.getDataModelName());
    }

    public final List<Document> validateDataAndCreateLog(Iterator<Row> itr, Map<Integer, String> columnNames, Map<String, String> keyToValue, RequestModel requestModel, Map<String, Boolean> mandatoryColumns,boolean onlyValidate,Sheet xSheet) throws FileOperationsException {
        ArrayList<Document> documentList = new ArrayList<>();
        ArrayList<Document> errorDocumentList = new ArrayList<>();
        boolean partialSuccess = false;
        boolean createLogFile = false;
        int uploadedRecords = 0;
        int failedRecords = 0;
        while (itr.hasNext()) {
            Row row = itr.next();
            if (row.getRowNum() != 0) {
                Iterator<Cell> cellIterator = row.cellIterator();
                var document = new Document();
                StringBuilder error = new StringBuilder();
                boolean rowHasError = false;
                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();
                    String header = columnNames.get(cell.getColumnIndex());
                    switch (cell.getCellType().toString()) {
                        case "STRING":
                            Optional<Map.Entry<String, String>> entryString = keyToValue.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(header) && !"STRING".equalsIgnoreCase(getJavaType(e.getValue()))).findAny();
                            if(entryString.isEmpty() && isDate(cell.getStringCellValue())){
                                Map<String,String> map = new HashMap<>();
                                map.put(header,"Edm.String");
                                Map.Entry<String, String> specificEntry = getSpecificEntry(map, header);
                                Optional<Map.Entry<String, String>> specialEntry = Optional.ofNullable(specificEntry);
                                rowHasError = isRowHasError(error, rowHasError, specialEntry, "Date");
                            }
                            if(entryString.isPresent() && !"Date".equalsIgnoreCase(getJavaType(entryString.get().getValue()))) {
                                rowHasError = isRowHasError(error, rowHasError, entryString, "String");
                            }
                            document.append(columnNames.get(cell.getColumnIndex()), cell.getStringCellValue());
                            break;
                        case "NUMERIC":
                            if(DateUtil.isCellDateFormatted(cell)){
                                Optional<Map.Entry<String, String>> entryNumeric = keyToValue.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(header) && !"DATE".equalsIgnoreCase(getJavaType(e.getValue()))).findAny();
                                rowHasError = isRowHasError(error, rowHasError, entryNumeric, "Date");
                                Date dateValue = cell.getDateCellValue();
                                document.append(columnNames.get(cell.getColumnIndex()), dateValue);
                            } else {
                                Optional<Map.Entry<String, String>> entryNumeric = keyToValue.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(header) && !"NUMERIC".equalsIgnoreCase(getJavaType(e.getValue()))).findAny();
                                rowHasError = isRowHasError(error, rowHasError, entryNumeric, "Numeric");
                                document.append(columnNames.get(cell.getColumnIndex()), cell.getNumericCellValue());
                            }
                            break;
                        case "BLANK":
                            document.append(columnNames.get(cell.getColumnIndex()), "");
                            if (Boolean.TRUE.equals(mandatoryColumns.get(header))) {
                                rowHasError = true;
                                Optional<Map.Entry<String, String>> entryBlank = keyToValue.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(header)).findAny();
                                if (entryBlank.isPresent()) {
                                    if (error.length() > 0) {
                                        error.append(" and ").append(entryBlank.get().getKey()).append(" columns ").append(" is mandatory,So the value at this column cannot be null or blank.");
                                    } else {
                                        error.append(entryBlank.get().getKey()).append(" columns ").append(" is mandatory,So the value at this column cannot be null or blank.");
                                    }
                                }
                            }
                            break;
                        case "BOOLEAN":
                            Optional<Map.Entry<String, String>> entryBoolean = keyToValue.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(header) && !"BOOLEAN".equalsIgnoreCase(getJavaType(e.getValue()))).findAny();
                            rowHasError = isRowHasError(error, rowHasError, entryBoolean, "Boolean");
                            document.append(columnNames.get(cell.getColumnIndex()), cell.getBooleanCellValue());
                            break;
                        case "FORMULA":
                            switch (cell.getCachedFormulaResultType().toString()) {
                                case "STRING":
                                    Optional<Map.Entry<String, String>> entryStringFormula = keyToValue.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(header) && !"STRING".equalsIgnoreCase(getJavaType(e.getValue()))).findAny();
                                    rowHasError = isRowHasError(error, rowHasError, entryStringFormula, "String");
                                    document.append(columnNames.get(cell.getColumnIndex()), cell.getStringCellValue());
                                    break;
                                case "NUMERIC":
                                    Optional<Map.Entry<String, String>> entryNumericFormula = keyToValue.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(header) && !"NUMERIC".equalsIgnoreCase(getJavaType(e.getValue()))).findAny();
                                    rowHasError = isRowHasError(error, rowHasError, entryNumericFormula, "Numeric");
                                    document.append(columnNames.get(cell.getColumnIndex()), cell.getNumericCellValue());
                                    break;
                                case "BOOLEAN":
                                    Optional<Map.Entry<String, String>> entryBooleanFormula = keyToValue.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(header) && !"BOOLEAN".equalsIgnoreCase(getJavaType(e.getValue()))).findAny();
                                    rowHasError = isRowHasError(error, rowHasError, entryBooleanFormula, "Boolean");
                                    document.append(columnNames.get(cell.getColumnIndex()), cell.getBooleanCellValue());
                                    break;
                                case "BLANK":
                                    document.append(columnNames.get(cell.getColumnIndex()), "");
                                    if (Boolean.TRUE.equals(mandatoryColumns.get(header))) {
                                        rowHasError = true;
                                        Optional<Map.Entry<String, String>> entryBlankFormula = keyToValue.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(header)).findAny();
                                        if (entryBlankFormula.isPresent()) {
                                            if (error.length() > 0) {
                                                error.append(" and ").append(entryBlankFormula.get().getKey()).append(" columns ").append(" value is null or blank");
                                            } else {
                                                error.append(entryBlankFormula.get().getKey()).append(" columns ").append(" value is null or blank");
                                            }
                                        }
                                    }
                                    break;
                                default:
                            }
                            break;
                        default:
                    }
                }
                if (rowHasError) {
                    document = addErrorToDocument(document,  columnNames);
                    document.append("Reason", error.toString());
                    errorDocumentList.add(document);
                    createLogFile = true;
                    failedRecords++;
                } else {
                    documentList.add(document);
                    partialSuccess = true;
                    uploadedRecords++;
                }
            }
        }
        if (!onlyValidate && !documentList.isEmpty()) {
            String collectionName = requestModel.getWorkspaceName() + "-" + requestModel.getMiniAppName();
            var database = mongodbClient.getDatabase(collectionName);
            List<WriteModel<Document>> bulkOperations = new ArrayList<>();
            for (Document document : documentList) {
                UpdateOneModel<Document> updateModel = new UpdateOneModel<>(document, new Document("$set", document), new UpdateOptions().upsert(true));
                bulkOperations.add(updateModel);
            }
            BulkWriteResult bulkWriteResult = database.getCollection(xSheet.getSheetName()).bulkWrite(bulkOperations);
            Log.info("Inserted Count: " + bulkWriteResult.getInsertedCount());
            Log.info("Matched Count: " + bulkWriteResult.getMatchedCount());
            Log.info("Modified Count: " + bulkWriteResult.getModifiedCount());
        }

        if (createLogFile) {
            uploadErrorToDBAndLogStatus(errorDocumentList, requestModel, !partialSuccess,uploadedRecords,failedRecords);
        }
        return documentList;
    }

    private static Map.Entry<String, String> getSpecificEntry(Map<String, String> map, String key) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (entry.getKey().equals(key)) {
                return entry;
            }
        }
        return null;
    }

    private static boolean isDate(String value) {
        String[] dateFormats = {
                "yyyy-MM-dd",
                "MM/dd/yyyy",
                "dd/MM/yyyy",
                "yyyy-MM-dd HH:mm",
                "dd-MM-yyyy HH:mm:ss"
        };

        for (String format : dateFormats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                sdf.setLenient(false);
                sdf.parse(value);
                return true;
            } catch (ParseException e) {
               // The String was not of type date
            }
        }
        return false;
    }

    private Document addErrorToDocument(Document document, Map<Integer, String> columnNames){
        var doc = new Document();
        for(Map.Entry<Integer, String> columns : columnNames.entrySet()){
            doc.put(columns.getValue(), document.getOrDefault(columns.getValue(), ""));
        }
        return doc;
    }

    private boolean isRowHasError(StringBuilder error, boolean rowHasError, Optional<Map.Entry<String, String>> entryString, String value) {
        if (entryString.isPresent()) {
            rowHasError = true;
            if (error.length() > 0) {
                error.append(" and ").append(entryString.get().getKey()).append(" columns data type is ").append(value).append(" in the uploaded file, where as the data type present in the model for this column is ").append(getJavaType(entryString.get().getValue()));
            } else {
                error.append(entryString.get().getKey()).append(" columns data type is ").append(value).append(" in the uploaded file, where as the data type present in the model for this column is ").append(getJavaType(entryString.get().getValue()));
            }
        }
        return rowHasError;
    }

    private Map<Map<String, Boolean>, String> parseXML(String input, String sheetName, RequestModel requestModel) throws ParserConfigurationException, IOException, SAXException, FileOperationsException {
        Map<Map<String, Boolean>, String> keyToValue = new HashMap<>();
        DocumentBuilderFactory documentBuilderFactory = new DocumentBuilderFactoryImpl();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        var document = documentBuilder.parse(new InputSource(new StringReader(input)));
        document.getDocumentElement().normalize();
        NodeList nodeList = document.getElementsByTagName("EntityType");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String collectionNameFromXML = element.getAttribute("Name");
                if (collectionNameFromXML.equalsIgnoreCase(sheetName)) {
                    NodeList nodeList1 = element.getChildNodes();
                    for (int j = 0; j < nodeList1.getLength(); j++) {
                        Node node1 = nodeList1.item(j);
                        if (node1.getNodeType() == Node.ELEMENT_NODE) {
                            Element element1 = (Element) node1;
                            if (element1.getTagName().equalsIgnoreCase("Property")) {
                                Map<String, Boolean> map = new HashMap<>();
                                map.put(element1.getAttribute("Name"), false);
                                if (element1.hasAttribute("Nullable") && element1.getAttribute("Nullable").equalsIgnoreCase("false")) {
                                    map.put(element1.getAttribute("Name"), true);
                                }
                                keyToValue.put(map, element1.getAttribute("Type"));
                            }
                        }
                    }
                }
            }
        }
        if (keyToValue.isEmpty()) {
            List<Document> documentList = new ArrayList<>();
            Document document1 = new Document();
            document1.append("Reason", "The sheet name and the data model name do not match");
            documentList.add(document1);
            uploadErrorToDBAndLogStatus(documentList, requestModel,true,0,0);
        }
        return keyToValue;
    }

    private byte[] createLogFile(Map<Integer, List<String>> data, String sheetName) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet spreadsheet = workbook.createSheet(sheetName);
            Set<Integer> keyId = data.keySet();
            int rowId = 0;
            for (Integer key : keyId) {
                XSSFRow row = spreadsheet.createRow(rowId++);
                List<String> list = data.get(key);
                int cellId = 0;
                for (String obj : list) {
                    Cell cell = row.createCell(cellId++);
                    cell.setCellValue(obj);
                }

                // Add the following code to set the last column to red
                int lastCellNum = row.getLastCellNum();
                if (lastCellNum >= 0) {
                    Cell lastCell = row.getCell(lastCellNum - 1);
                    CellStyle style = workbook.createCellStyle();
                    style.setFillForegroundColor(IndexedColors.RED.getIndex());
                    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    lastCell.setCellStyle(style);
                }
            }
            workbook.write(bos);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bos.toByteArray();
    }

    private static String getJavaType(String type) {
        switch (type) {
            case "Edm.Boolean":
                return "BOOLEAN";
            case "Edm.SByte":
            case "Edm.Int32":
            case "Edm.Double":
                return "NUMERIC";
            case "Edm.Date":
                return "DATE";
            default:
                return "STRING";
        }
    }

    public final long nextUploadId() {
        IdCountPerRequest idCountPerRequest = idCountPerRequestRepository.getRecord();
        if (idCountPerRequest != null){
            long count = idCountPerRequest.getRequestCount() + 1;
            idCountPerRequest.setRequestCount(count);
            idCountPerRequestRepository.updateRecord(idCountPerRequest);
            return count;
        }else {
            idCountPerRequest = new IdCountPerRequest(1);
            idCountPerRequestRepository.save(idCountPerRequest);
            return 1;
        }
    }

    public final long nextTemplateId() {
        return templateQueueRepository.getCount() + 1;
    }

    public final boolean collectionExists(RequestModel requestModel) {
        String collectionName = (requestModel.getWorkspaceName() + "-" + requestModel.getMiniAppName() + "-" + requestModel.getUploadId()).toLowerCase();
        var database = mongodbClient.getDatabase("errorLogs");
        for (String name : database.listCollectionNames()) {
            if (name.equalsIgnoreCase(collectionName)) {
                return true;
            }
        }
        return false;
    }

    public final RequestModel addRequestToQueue(RequestModel requestModel, boolean addToQueue) throws IOException {
        InputStream inputStream = new ByteArrayInputStream(requestModel.getExcelContent());
        XSSFWorkbook xlBook = new XSSFWorkbook(inputStream);
        for (Sheet xSheet : xlBook) {
            int totalRows = xSheet.getLastRowNum();
            long nextUploadId = nextUploadId();
            Path fileName = Path.of(
                    fileLocation + requestModel.getWorkspaceName() + "-" + requestModel.getMiniAppName() + "-" + nextUploadId + ".xlsx");
            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                Iterator<Row> itr = xSheet.iterator();
                XSSFSheet spreadsheet
                        = workbook.createSheet(xSheet.getSheetName());
                int rowId = 0;
                int size = 0;
                while (itr.hasNext()) {
                    Row rowItr = itr.next();
                    if(rowItr.getRowNum() == 0){
                        size = rowItr.getLastCellNum();
                    }
                    XSSFRow row = spreadsheet.createRow(rowId++);
                    int effectiveRowIdx = 0;
                    for( int i = 0 ; i < size ; i++){
                        Cell cellItr = rowItr.getCell(i);
                        if(xSheet.getRow(0) == null ||
                                xSheet.getRow(0).getCell(i) == null ||
                                xSheet.getRow(0).getCell(i).getStringCellValue().isEmpty()){
                            continue;
                        }
                        Cell cell = row.createCell(effectiveRowIdx++);
                        if (cellItr == null) {
                            cell.setBlank();
                            continue;
                        }
                        switch (cellItr.getCellType().toString()) {
                            case "STRING":
                                cell.setCellValue(cellItr.getStringCellValue());
                                break;
                            case "NUMERIC":
                                if(DateUtil.isCellDateFormatted(cellItr)){
                                    Date dateValue = cellItr.getDateCellValue();
                                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                                    String formattedDate = dateFormat.format(dateValue);
                                    cell.setCellValue(formattedDate);
                                } else {
                                    cell.setCellValue(cellItr.getNumericCellValue());
                                }
                                break;
                            case "BLANK":
                                cell.setBlank();
                                break;
                            case "BOOLEAN":
                                cell.setCellValue(cellItr.getBooleanCellValue());
                                break;
                            case "FORMULA":
                                FormulaEvaluator formulaEvaluator = xlBook.getCreationHelper().createFormulaEvaluator();
                                CellValue cellValue = formulaEvaluator.evaluate(cellItr);
                                switch (cellValue.getCellType().toString()) {
                                    case "NUMERIC":
                                        cell.setCellValue(cellValue.getNumberValue());
                                        break;
                                    case "STRING":
                                        cell.setCellValue(cellValue.getStringValue());
                                        break;
                                    case "BOOLEAN":
                                        cell.setCellValue(cellValue.getBooleanValue());
                                        break;
                                    case "BLANK":
                                        cell.setBlank();
                                        break;
                                    default:
                                        break;
                                }
                                break;
                            default:
                        }
                    }
                }
                if(addToQueue) {
                    FileOutputStream fileOut = new FileOutputStream(fileName.toFile());
                    workbook.write(fileOut);
                    fileOut.close();
                    ProcessQueue processQueue = new ProcessQueue(requestModel.getMiniAppName(), requestModel.getWorkspaceName(), Status.QUEUED, nextUploadId, new Date(), new Date(), requestModel.getUploadedBy(), xSheet.getSheetName());
                    processQueue.setSuccessRecordsCount(totalRows);
                    processQueueRepository.saveProcessQueue(processQueue);
                }
                requestModel.setTotalRecords(totalRows);
            }
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        xlBook.write(byteArrayOutputStream);
        xlBook.close();
        byte[] workbookByteArray = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();
        requestModel.setExcelContent(workbookByteArray);
        return requestModel;
    }

    public final RequestModel convertRowsToColumns(RequestModel requestModel, boolean addToQueue) throws IOException {
        InputStream inputStream = new ByteArrayInputStream(requestModel.getExcelContent());
        XSSFWorkbook xlBook = new XSSFWorkbook(inputStream);
        for (Sheet xSheet : xlBook) {
            try (Workbook outputWorkbook = new XSSFWorkbook()) {
                Sheet outputSheet = outputWorkbook.createSheet(xSheet.getSheetName());
                int totalRows = xSheet.getLastRowNum();
                long nextUploadId = nextUploadId();
                int totalColumns = 0;
                for (Row row : xSheet) {
                    totalColumns = Math.max(totalColumns, row.getLastCellNum());
                }
                for (int colIdx = 0; colIdx < totalColumns; colIdx++) {
                    Row outputRow = outputSheet.createRow(colIdx);
                    int effectiveRowId = 0;
                    for (int rowIdx = 0; rowIdx <= totalRows; rowIdx++) {
                        Row inputRow = xSheet.getRow(rowIdx);
                        if (inputRow == null || inputRow.getCell(0).getStringCellValue().isEmpty()) {
                            continue;
                        }
                        Cell inputCell = inputRow.getCell(colIdx);
                        Cell outputCell = outputRow.createCell(effectiveRowId++);
                        if (inputCell != null) {
                            switch (inputCell.getCellType()) {
                                case STRING:
                                    outputCell.setCellValue(inputCell.getStringCellValue());
                                    break;
                                case NUMERIC:
                                    if (DateUtil.isCellDateFormatted(inputCell)) {
                                        Date dateValue = inputCell.getDateCellValue();
                                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                                        String formattedDate = dateFormat.format(dateValue);
                                        outputCell.setCellValue(formattedDate);
                                    } else {
                                        outputCell.setCellValue(inputCell.getNumericCellValue());
                                    }
                                    break;
                                case BLANK:
                                    outputCell.setBlank();
                                    break;
                                case BOOLEAN:
                                    outputCell.setCellValue(inputCell.getBooleanCellValue());
                                    break;
                                case FORMULA:
                                    FormulaEvaluator formulaEvaluator = xlBook.getCreationHelper().createFormulaEvaluator();
                                    CellValue cellValue = formulaEvaluator.evaluate(inputCell);
                                    switch (cellValue.getCellType()) {
                                        case NUMERIC:
                                            outputCell.setCellValue(cellValue.getNumberValue());
                                            break;
                                        case STRING:
                                            outputCell.setCellValue(cellValue.getStringValue());
                                            break;
                                        case BOOLEAN:
                                            outputCell.setCellValue(cellValue.getBooleanValue());
                                            break;
                                        case BLANK:
                                            outputCell.setBlank();
                                            break;
                                        default:
                                            break;
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }

                if (addToQueue) {
                    Path fileName = Path.of(
                            fileLocation + requestModel.getWorkspaceName() + "-" + requestModel.getMiniAppName() + "-" + nextUploadId + ".xlsx");
                    FileOutputStream fileOut = new FileOutputStream(fileName.toFile());
                    outputWorkbook.write(fileOut);
                    fileOut.close();
                    ProcessQueue processQueue = new ProcessQueue(requestModel.getMiniAppName(), requestModel.getWorkspaceName(), Status.QUEUED, nextUploadId, new Date(), new Date(), requestModel.getUploadedBy(), xSheet.getSheetName());
                    processQueue.setSuccessRecordsCount(totalRows);
                    processQueueRepository.saveProcessQueue(processQueue);
                }
                requestModel.setTotalRecords(totalColumns);
            }
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        xlBook.write(byteArrayOutputStream);
        xlBook.close();
        byte[] workbookByteArray = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();
        requestModel.setExcelContent(workbookByteArray);
        return requestModel;
    }

}
