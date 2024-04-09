Topics:
1. Validations
2. APIS
3. Architecture
4. API’s details

Validations:
1. The headers in the Excel file should be the same as the data model headers from the odata services, spaces should be
ignored(currently working on ignoring characters).
2. The data type in the Excel file should be the same as the data model data type from the odata services.
3. The sheet name should be the same as the entity types from odata services.

APIS:
1. Save data API: This API will first validate the data and then the data into mongodb {URL}/upload-file/saveData

2. To validate only API will only validate the data present in the Excel sheet {URL}/upload-file/validateData

3. The status API:

   - Will fetch all the uploaded file with the status
     {URL}/upload-file/getAllUploadsStatus
   
   - Will fetch the particular uploads' status
     {URL}/upload-file/getParticularUploadsStatus
      
   - We are storing the validations errors in the mongodb Db, In order to show this error to the user we have implemented
      this API. This will store the validation errors on the bases workspace-app name-sheetname. If these three things
      are passed from UI, The validation error which was thrown for these combinations will be retrieved in the form of a
      byte array.
      {URL}/upload-file/errorLogFile

4. To get the Excel template.


Architecture:
    All the same files uploaded from the same user will be queued and processed sequentially. The queue is the database
    table, we are putting all the requests in the table and scanning it every 5 seconds. If the status of one of the
    processes with the same name is in the running state then no other processes of the same user will be processed.
    The files for these processes are stored in the server, once the process is completed the file will be deleted.
    API’s Explanation:


1. Save data API: This API will first validate the data and then the data into mongodb {URL}/upload-file/saveData.
   Type: POST
   Request body : {
      "workspaceName": "work space name",
      "miniAppName": "mini app name",
      "excelContent": "Excel sheet file content"
   }
   Response :
    {
      "workspaceName": "work space name",
      "miniAppName": “mini app name‘,
      "uploadId": 14,
      "status": "QUEUED",
      "remark": "The request has been added to the process queue, It will processed soon."
    }

2. To validate only API will only validate the data present in the excel sheet {URL}/upload-file/validateData
    Type: POST
    Request Body : {
    "workspaceName": "work space name",
    "miniAppName": "mini app name",
    "excelContent": "Excel sheet file content"
    }
    Response: {
      "workspaceName": "VOS",
      "miniAppName": "infinity",
      "uploadId": 15,
      "sheetName": "sheet2",
      "status": "SUCCESS",
      "remark": "The data was successfully uploaded"
    }

3. The status API:
   - Will fetch all the records status
   {URL}/upload-file/getAllUploadsStatus
   Type: GET
   Request body : {}
   Response : [
    {
      "workspaceName": "VOS",
      "miniAppName": "infinity",
      "excelContent": null,
      "uploadId": 2,
      "sheetName": "sheet2",
      "date": "2022-10-11T20:40:24.684+05:30",
      "status": "FAILED",
      "remark": null
    },
   {
     "workspaceName": "VOS",
     "miniAppName": "infinity",
     "excelContent": null,
     "uploadId": 1,
     "sheetName": "Sheet1",
     "date": "2022-10-11T20:40:23.846+05:30",
     "status": "SUCCESS",
     "remark": null
   }
  ]
   - Will fetch the particular uploads status:
     {URL}/upload-file/getParticularUploadsStatus
     Type: GET
     Request body:    {
         "workspaceName": "VOS",
         "miniAppName": "infinity",
         "uploadId": 2
     }
  Response:[
  {
  "workspaceName": "VOS",
  "miniAppName": "infinity",
  "excelContent": null,
  "uploadId": 2,
  "sheetName": "sheet2",
  "date": "2022-10-11T20:40:24.684+05:30",
  "status": "FAILED",
  "remark": null
  }
  ]

4. Api to get the error log of the file uploaded:
   The response will have the error log data in the form of byte array 
   - {URL}/upload-file/errorLogFile
      Type: POST
      Request body:   {
         "workspaceName": "VOS",
         "miniAppName": "infinity",
         "uploadId": 2
      }
      Response:{
          "workspaceName": "VOS",
          "miniAppName": "infinity",
          "excelContent": "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",
      }

5. To get the Excel template:
 - {URL}/upload-file/getExcelTemplate
   Type: GET
   Request body: {}
   Response: File will be downloaded.