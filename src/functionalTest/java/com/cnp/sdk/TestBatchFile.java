package com.cnp.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;

import javax.xml.bind.JAXBException;

import com.cnp.sdk.generate.*;
import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.junit.Ignore;
import org.junit.Test;

public class TestBatchFile {
    private final long TIME_STAMP = System.currentTimeMillis();
    @Test
    public void testSendToCnp_WithFileConfig() throws Exception {
        String requestFileName = "cnpSdk-testBatchFile-fileConfig-" + TIME_STAMP + ".xml";
        CnpBatchFileRequest request = new CnpBatchFileRequest(
                requestFileName);

        // request file is being set in the constructor
        assertNotNull(request.getFile());

        Properties configFromFile = request.getConfig();

        // pre-assert the config file has required param values
        assertEquals("payments.vantivprelive.com",
                configFromFile.getProperty("batchHost"));
        //assertEquals("15000", configFromFile.getProperty("batchPort"));

        String workingDirRequests = configFromFile
                .getProperty("batchRequestFolder");
        prepDir(workingDirRequests);

        String workingDirResponses = configFromFile
                .getProperty("batchResponseFolder");
        prepDir(workingDirResponses);

        prepareTestRequest(request);

        /* call method under test */
        CnpBatchFileResponse response = request.sendToCnpSFTP();//sendToCnp();

        // assert response can be processed through Java API
        assertJavaApi(request, response);

        // assert request and response files were created properly
        assertGeneratedFiles(workingDirRequests, workingDirResponses,
                requestFileName, request, response);
    }

    @Test
    public void testSendToCnp_WithConfigOverrides() throws Exception {
        String workingDir = System.getProperty("java.io.tmpdir");

        String workingDirRequests = workingDir + File.separator
                + "cnpSdkTestBatchRequests";
        prepDir(workingDirRequests);

        String workingDirResponses = workingDir + File.separator
                + "cnpSdkTestBatchResponses";
        prepDir(workingDirResponses);

        Properties configOverrides = new Properties();
        configOverrides.setProperty("batchHost", "payments.vantivprelive.com ");
        configOverrides.setProperty("sftpTimeout", "720000");

        configOverrides.setProperty("batchRequestFolder", workingDirRequests);
        configOverrides.setProperty("batchResponseFolder", workingDirResponses);

        String requestFileName = "cnpSdk-testBatchFile-configOverridesSFTP-" + TIME_STAMP + ".xml";
        CnpBatchFileRequest request = new CnpBatchFileRequest(
                requestFileName, configOverrides);

        // request file is being set in the constructor
        assertNotNull(request.getFile());

        prepareTestRequest(request);

        // actually add a transaction

        /* call method under test */
        CnpBatchFileResponse response = request.sendToCnpSFTP();

        // assert response can be processed through Java API
        assertJavaApi(request, response);

        // assert request and response files were created properly
        assertGeneratedFiles(workingDirRequests, workingDirResponses,
                requestFileName, request, response);
    }

    @Test
    public void testSendToCnpSFTP_WithPreviouslyCreatedFile()
            throws Exception {
        String requestFileName = "cnpSdk-testBatchFile-fileConfigSFTP-" + TIME_STAMP + ".xml";
        CnpBatchFileRequest request = new CnpBatchFileRequest(
                requestFileName);

        // request file is being set in the constructor
        assertNotNull(request.getFile());

        Properties configFromFile = request.getConfig();

        // pre-assert the config file has required param values
        assertEquals("payments.vantivprelive.com",
                configFromFile.getProperty("batchHost"));
        // assertEquals("15000", configFromFile.getProperty("batchPort"));

        String workingDirRequests = configFromFile
                .getProperty("batchRequestFolder");
        prepDir(workingDirRequests);

        String workingDirResponses = configFromFile
                .getProperty("batchResponseFolder");
        prepDir(workingDirResponses);

        prepareTestRequest(request);

        // This should have generated the file
        request.prepareForDelivery();

        // Make sure the file exists
        File requestFile = request.getFile();
        assertTrue(requestFile.exists());
        assertTrue(requestFile.length() > 0);

        CnpBatchFileRequest request2 = new CnpBatchFileRequest(
                requestFileName);

        CnpBatchFileResponse response = request2.sendToCnpSFTP(true);

        // Assert response matches what was requested
        assertJavaApi(request2, response);

        // Make sure files were created correctly
        assertGeneratedFiles(workingDirRequests, workingDirResponses,
                requestFileName, request2, response);
    }

    @Test
    public void testSendOnlyToCnpSFTP_WithPreviouslyCreatedFile()
            throws Exception {
        // --- Prepare the batch file ---
        String requestFileName = "cnpSdk-testBatchFile-fileConfigSFTP-" + TIME_STAMP + ".xml";
        CnpBatchFileRequest request1 = new CnpBatchFileRequest(
                requestFileName);

        // request file is being set in the constructor
        assertNotNull(request1.getFile());

        Properties configFromFile = request1.getConfig();

        // pre-assert the config file has required param values
        assertEquals("payments.vantivprelive.com",
                configFromFile.getProperty("batchHost"));
       // assertEquals("15000", configFromFile.getProperty("batchPort"));

        String workingDirRequests = configFromFile
                .getProperty("batchRequestFolder");
        prepDir(workingDirRequests);

        String workingDirResponses = configFromFile
                .getProperty("batchResponseFolder");
        prepDir(workingDirResponses);

        prepareTestRequest(request1);

        // This should have generated the file
        request1.prepareForDelivery();

        // Make sure the file exists
        File requestFile1 = request1.getFile();
        assertTrue(requestFile1.exists());
        assertTrue(requestFile1.length() > 0);

        // --- Send the batch to Cnp's SFTP ---

        // Move request file to temporary location
        File requestFile2 = File.createTempFile("cnp", ".xml");
        copyFile(requestFile1, requestFile2);

        Properties configForRequest2 = (Properties) configFromFile.clone();
        configForRequest2.setProperty("batchRequestFolder", requestFile2
                .getParentFile().getCanonicalPath());

        CnpBatchFileRequest request2 = new CnpBatchFileRequest(
                requestFile2.getName(), configForRequest2);
        request2.sendOnlyToCnpSFTP(true);

        // --- Retrieve response ---
        CnpBatchFileRequest request3 = new CnpBatchFileRequest(
                requestFile2.getName(), configForRequest2);
        CnpBatchFileResponse response = request3.retrieveOnlyFromCnpSFTP();

        // Assert response matches what was requested
        assertJavaApi(request3, response);

        // Make sure files were created correctly
        assertGeneratedFiles(requestFile2.getParentFile().getCanonicalPath(),
                workingDirResponses, requestFile2.getName(), request3, response);
    }

    @Test
    public void testSendToCnpSFTP_WithFileConfig() throws Exception {
        String requestFileName = "cnpSdk-testBatchFile-fileConfigSFTP-" + TIME_STAMP + ".xml";
        CnpBatchFileRequest request = new CnpBatchFileRequest(
                requestFileName);

        // request file is being set in the constructor
        assertNotNull(request.getFile());

        Properties configFromFile = request.getConfig();

        // pre-assert the config file has required param values
        assertEquals("payments.vantivprelive.com",
                configFromFile.getProperty("batchHost"));
       // assertEquals("15000", configFromFile.getProperty("batchPort"));

        String workingDirRequests = configFromFile
                .getProperty("batchRequestFolder");
        prepDir(workingDirRequests);

        String workingDirResponses = configFromFile
                .getProperty("batchResponseFolder");
        prepDir(workingDirResponses);

        prepareTestRequest(request);

        /* call method under test */
        CnpBatchFileResponse response = request.sendToCnpSFTP();

        // assert response can be processed through Java API
        assertJavaApi(request, response);

        // assert request and response files were created properly
        assertGeneratedFiles(workingDirRequests, workingDirResponses,
                requestFileName, request, response);
    }

    @Test
    public void testSendToCnpSFTP_WithConfigOverrides() throws Exception {
        String workingDir = System.getProperty("java.io.tmpdir");

        String workingDirRequests = workingDir + File.separator
                + "cnpSdkTestBatchRequests";
        prepDir(workingDirRequests);

        String workingDirResponses = workingDir + File.separator
                + "cnpSdkTestBatchResponses";
        prepDir(workingDirResponses);

        Properties configOverrides = new Properties();
        configOverrides.setProperty("batchHost", "payments.vantivprelive.com");
        configOverrides.setProperty("sftpTimeout", "720000");

        configOverrides.setProperty("batchRequestFolder", workingDirRequests);
        configOverrides.setProperty("batchResponseFolder", workingDirResponses);

        String requestFileName = "cnpSdk-testBatchFile-configOverridesSFTP-" + TIME_STAMP + ".xml";
        CnpBatchFileRequest request = new CnpBatchFileRequest(
                requestFileName, configOverrides);

        // request file is being set in the constructor
        assertNotNull(request.getFile());

        prepareTestRequest(request);

        /* call method under test */
        CnpBatchFileResponse response = request.sendToCnpSFTP();

        // assert response can be processed through Java API
        assertJavaApi(request, response);

        // assert request and response files were created properly
    }

    private void prepareTestRequest(CnpBatchFileRequest request)
            throws FileNotFoundException, JAXBException {

        Properties configFromFile = request.getConfig();

        CnpBatchRequest batchRequest1 = request.createBatch(configFromFile.getProperty("merchantId"));
        Sale sale11 = new Sale();
        sale11.setReportGroup("reportGroup11");
        sale11.setOrderId("orderId11");
        sale11.setAmount(1099L);
        sale11.setOrderSource(OrderSourceType.ECOMMERCE);
        sale11.setId("id");
//        PinlessDebitRequestType pinlessDebitRequest = new PinlessDebitRequestType();
//        pinlessDebitRequest.setRoutingPreference(RoutingPreferenceEnum.REGULAR);
//        PreferredDebitNetworksType preferredDebitNetwork = new PreferredDebitNetworksType();
//        preferredDebitNetwork.getDebitNetworkNames().add("Visa");
//        pinlessDebitRequest.setPreferredDebitNetworks(preferredDebitNetwork);
//        sale11.setPinlessDebitRequest(pinlessDebitRequest);

        CardType card = new CardType();
        card.setType(MethodOfPaymentTypeEnum.VI);
        card.setNumber("4457010000000009");
        card.setExpDate("0114");
        card.setPin("1234");
        sale11.setCard(card);

        batchRequest1.addTransaction(sale11);
    }

    @Test
    public void testMechaBatchAndProcess() {
        String requestFileName = "cnpSdk-testBatchFile-MECHA-" + TIME_STAMP + ".xml";
        CnpBatchFileRequest request = new CnpBatchFileRequest(
                requestFileName);

        Properties configFromFile = request.getConfig();

        // pre-assert the config file has required param values
        assertEquals("payments.vantivprelive.com",
                configFromFile.getProperty("batchHost"));
       // assertEquals("15000", configFromFile.getProperty("batchPort"));

        CnpBatchRequest batch = request.createBatch(configFromFile.getProperty("merchantId"));

        // card
        CardType card = new CardType();
        card.setNumber("4100000000000001");
        card.setExpDate("1210");
        card.setType(MethodOfPaymentTypeEnum.VI);

        // echeck
        EcheckType echeck = new EcheckType();
        echeck.setAccNum("1234567890");
        echeck.setAccType(EcheckAccountTypeEnum.CHECKING);
        echeck.setRoutingNum("123456789");
        echeck.setCheckNum("123455");

        // billto address
        Contact contact = new Contact();
        contact.setName("Bob");
        contact.setCity("Lowell");
        contact.setState("MA");
        contact.setEmail("Bob@cnp.com");

        Authorization auth = new Authorization();
        auth.setReportGroup("Planets");
        auth.setOrderId("12344");
        auth.setAmount(106L);
        auth.setOrderSource(OrderSourceType.ECOMMERCE);
        auth.setCard(card);
        auth.setId("id");
        LodgingInfo lodgingInfo = new LodgingInfo();
        lodgingInfo.setRoomRate(106L);
        lodgingInfo.setRoomTax(0L);
        LodgingCharge lodgingCharge = new LodgingCharge();
        lodgingCharge.setName(LodgingExtraChargeEnum.RESTAURANT);
        lodgingInfo.getLodgingCharges().add(lodgingCharge);
        auth.setLodgingInfo(lodgingInfo);
        batch.addTransaction(auth);

        Sale sale = new Sale();
        sale.setReportGroup("Planets");
        sale.setOrderId("12344");
        sale.setAmount(6000L);
        sale.setOrderSource(OrderSourceType.ECOMMERCE);
        sale.setCard(card);
        sale.setId("id");
        batch.addTransaction(sale);

        Credit credit = new Credit();
        credit.setReportGroup("Planets");
        credit.setOrderId("12344");
        credit.setAmount(106L);
        credit.setOrderSource(OrderSourceType.ECOMMERCE);
        credit.setCard(card);
        credit.setId("id");
        batch.addTransaction(credit);

        AuthReversal authReversal = new AuthReversal();
        authReversal.setReportGroup("Planets");
        authReversal.setCnpTxnId(12345678000L);
        authReversal.setAmount(106L);
        authReversal.setPayPalNotes("Notes");
        authReversal.setId("id");
        batch.addTransaction(authReversal);

        RegisterTokenRequestType registerTokenRequestType = new RegisterTokenRequestType();
        registerTokenRequestType.setReportGroup("Planets");
        registerTokenRequestType.setOrderId("12344");
        registerTokenRequestType.setAccountNumber("1233456789103801");
        registerTokenRequestType.setId("id");
        batch.addTransaction(registerTokenRequestType);

        UpdateCardValidationNumOnToken cardValidationNumOnToken = new UpdateCardValidationNumOnToken();
        cardValidationNumOnToken.setReportGroup("Planets");
        cardValidationNumOnToken.setId("12345");
        cardValidationNumOnToken.setCustomerId("0987");
        cardValidationNumOnToken.setOrderId("12344");
        cardValidationNumOnToken.setCnpToken("1233456789103801");
        cardValidationNumOnToken.setCardValidationNum("123");
        cardValidationNumOnToken.setId("id");
        batch.addTransaction(cardValidationNumOnToken);

        ForceCapture forceCapture = new ForceCapture();
        forceCapture.setReportGroup("Planets");
        forceCapture.setId("123456");
        forceCapture.setOrderId("12344");
        forceCapture.setAmount(106L);
        forceCapture.setOrderSource(OrderSourceType.ECOMMERCE);
        forceCapture.setCard(card);
        forceCapture.setId("id");
        batch.addTransaction(forceCapture);

        Capture capture = new Capture();
        capture.setReportGroup("Planets");
        capture.setCnpTxnId(123456000L);
        capture.setAmount(106L);
        capture.setId("id");
        batch.addTransaction(capture);

        CaptureGivenAuth captureGivenAuth = new CaptureGivenAuth();
        captureGivenAuth.setReportGroup("Planets");
        captureGivenAuth.setOrderId("12344");
        captureGivenAuth.setAmount(106L);
        AuthInformation authInformation = new AuthInformation();
        authInformation.setAuthDate(Calendar.getInstance());
        authInformation.setAuthAmount(12345L);
        authInformation.setAuthCode("543216");
        captureGivenAuth.setAuthInformation(authInformation);
        captureGivenAuth.setOrderSource(OrderSourceType.ECOMMERCE);
        captureGivenAuth.setCard(card);
        captureGivenAuth.setId("id");
        batch.addTransaction(captureGivenAuth);

        EcheckVerification echeckVerification = new EcheckVerification();
        echeckVerification.setReportGroup("Planets");
        echeckVerification.setAmount(123456L);
        echeckVerification.setOrderId("12345");
        echeckVerification.setOrderSource(OrderSourceType.ECOMMERCE);
        echeckVerification.setBillToAddress(contact);
        echeckVerification.setEcheck(echeck);
        echeckVerification.setId("id");
        batch.addTransaction(echeckVerification);

        EcheckCredit echeckCredit = new EcheckCredit();
        echeckCredit.setReportGroup("Planets");
        echeckCredit.setCnpTxnId(1234567890L);
        echeckCredit.setAmount(12L);
        echeckCredit.setId("id");
        batch.addTransaction(echeckCredit);

        EcheckRedeposit echeckRedeposit = new EcheckRedeposit();
        echeckRedeposit.setReportGroup("Planets");
        echeckRedeposit.setCnpTxnId(124321341412L);
        echeckRedeposit.setId("id");
        batch.addTransaction(echeckRedeposit);

        EcheckSale echeckSale = new EcheckSale();
        echeckSale.setReportGroup("Planets");
        echeckSale.setAmount(123456L);
        echeckSale.setOrderId("12345");
        echeckSale.setOrderSource(OrderSourceType.ECOMMERCE);
        echeckSale.setBillToAddress(contact);
        echeckSale.setEcheck(echeck);
        echeckSale.setVerify(true);
        echeckSale.setId("id");
        batch.addTransaction(echeckSale);

        int transactionCount = batch.getNumberOfTransactions();

        CnpBatchFileResponse fileResponse = request.sendToCnpSFTP();
        CnpBatchResponse batchResponse = fileResponse
                .getNextCnpBatchResponse();
        int txns = 0;

        ResponseValidatorProcessor processor = new ResponseValidatorProcessor();

        while (batchResponse.processNextTransaction(processor)) {
            txns++;
        }

        assertEquals(transactionCount, txns);
        assertEquals(transactionCount, processor.responseCount);
    }

    @Test
    public void testEcheckPreNoteAll() {
        String requestFileName = "cnpSdk-testBatchFile-EcheckPreNoteAll-" + TIME_STAMP + ".xml";
        CnpBatchFileRequest request = new CnpBatchFileRequest(
                requestFileName);

        Properties configFromFile = request.getConfig();



        // pre-assert the config file has required param values
        assertEquals("payments.vantivprelive.com",
                configFromFile.getProperty("batchHost"));
        //assertEquals("15000", configFromFile.getProperty("batchPort"));

        CnpBatchRequest batch = request.createBatch(configFromFile.getProperty("merchantId"));

        // echeck success
        EcheckType echeckSuccess = new EcheckType();
        echeckSuccess.setAccNum("1092969901");
        echeckSuccess.setAccType(EcheckAccountTypeEnum.CORPORATE);
        echeckSuccess.setRoutingNum("011075150");
        echeckSuccess.setCheckNum("123455");

        EcheckType echeckRoutErr = new EcheckType();
        echeckRoutErr.setAccNum("102969901");
        echeckRoutErr.setAccType(EcheckAccountTypeEnum.CORPORATE);
        echeckRoutErr.setRoutingNum("011100012");
        echeckRoutErr.setCheckNum("123455");

        EcheckType echeckAccErr = new EcheckType();
        echeckAccErr.setAccNum("6099999992");
        echeckAccErr.setAccType(EcheckAccountTypeEnum.CHECKING);
        echeckAccErr.setRoutingNum("053133052");
        echeckAccErr.setCheckNum("123455");

        // billto address
        Contact contact = new Contact();
        contact.setName("Bob");
        contact.setCity("Lowell");
        contact.setState("MA");
        contact.setEmail("Bob@cnp.com");

        EcheckPreNoteSale echeckPreNoteSaleSuccess = new EcheckPreNoteSale();
        echeckPreNoteSaleSuccess.setReportGroup("Planets");
        echeckPreNoteSaleSuccess.setOrderId("000");
        echeckPreNoteSaleSuccess.setBillToAddress(contact);
        echeckPreNoteSaleSuccess.setEcheck(echeckSuccess);
        echeckPreNoteSaleSuccess.setOrderSource(OrderSourceType.ECOMMERCE);
        echeckPreNoteSaleSuccess.setId("Id");
        batch.addTransaction(echeckPreNoteSaleSuccess);

        EcheckPreNoteSale echeckPreNoteSaleAccErr = new EcheckPreNoteSale();
        echeckPreNoteSaleAccErr.setReportGroup("Planets");
        echeckPreNoteSaleAccErr.setOrderId("301");
        echeckPreNoteSaleAccErr.setBillToAddress(contact);
        echeckPreNoteSaleAccErr.setEcheck(echeckAccErr);
        echeckPreNoteSaleAccErr.setOrderSource(OrderSourceType.ECOMMERCE);
        echeckPreNoteSaleAccErr.setId("Id");
        batch.addTransaction(echeckPreNoteSaleAccErr);

        EcheckPreNoteSale echeckPreNoteSaleRoutErr = new EcheckPreNoteSale();
        echeckPreNoteSaleRoutErr.setReportGroup("Planets");
        echeckPreNoteSaleRoutErr.setOrderId("900");
        echeckPreNoteSaleRoutErr.setBillToAddress(contact);
        echeckPreNoteSaleRoutErr.setEcheck(echeckRoutErr);
        echeckPreNoteSaleRoutErr.setOrderSource(OrderSourceType.ECOMMERCE);
        echeckPreNoteSaleRoutErr.setId("Id");
        batch.addTransaction(echeckPreNoteSaleRoutErr);

        EcheckPreNoteCredit echeckPreNoteCreditSuccess = new EcheckPreNoteCredit();
        echeckPreNoteCreditSuccess.setReportGroup("Planets");
        echeckPreNoteCreditSuccess.setOrderId("000");
        echeckPreNoteCreditSuccess.setBillToAddress(contact);
        echeckPreNoteCreditSuccess.setEcheck(echeckSuccess);
        echeckPreNoteCreditSuccess.setOrderSource(OrderSourceType.ECOMMERCE);
        echeckPreNoteCreditSuccess.setId("Id");
        batch.addTransaction(echeckPreNoteCreditSuccess);

        EcheckPreNoteCredit echeckPreNoteCreditAccErr = new EcheckPreNoteCredit();
        echeckPreNoteCreditAccErr.setReportGroup("Planets");
        echeckPreNoteCreditAccErr.setOrderId("301");
        echeckPreNoteCreditAccErr.setBillToAddress(contact);
        echeckPreNoteCreditAccErr.setEcheck(echeckAccErr);
        echeckPreNoteCreditAccErr.setOrderSource(OrderSourceType.ECOMMERCE);
        echeckPreNoteCreditAccErr.setId("Id");
        batch.addTransaction(echeckPreNoteCreditAccErr);

        EcheckPreNoteCredit echeckPreNoteCreditRoutErr = new EcheckPreNoteCredit();
        echeckPreNoteCreditRoutErr.setReportGroup("Planets");
        echeckPreNoteCreditRoutErr.setOrderId("900");
        echeckPreNoteCreditRoutErr.setBillToAddress(contact);
        echeckPreNoteCreditRoutErr.setEcheck(echeckRoutErr);
        echeckPreNoteCreditRoutErr.setOrderSource(OrderSourceType.ECOMMERCE);
        echeckPreNoteCreditRoutErr.setId("Id");
        batch.addTransaction(echeckPreNoteCreditRoutErr);

        TranslateToLowValueTokenRequestType translateToLowValueTokenRequest = new TranslateToLowValueTokenRequestType();
        translateToLowValueTokenRequest.setOrderId("123456789");
        translateToLowValueTokenRequest.setToken("qwe7895sdffd78598dsed8");
        translateToLowValueTokenRequest.setId("Id");
        translateToLowValueTokenRequest.setReportGroup("Planets");
        translateToLowValueTokenRequest.setCustomerId("12234");
        batch.addTransaction(translateToLowValueTokenRequest);

        int transactionCount = batch.getNumberOfTransactions();

        CnpBatchFileResponse fileResponse = request.sendToCnpSFTP();
        CnpBatchResponse batchResponse = fileResponse
                .getNextCnpBatchResponse();
        int txns = 0;

//        ResponseValidatorProcessor processor = new ResponseValidatorProcessor();

        while (batchResponse
                .processNextTransaction(new CnpResponseProcessor() {

                    public void processVendorDebitResponse(
                            VendorDebitResponse vendorDebitResponse) {
                    }

                    public void processVendorCreditResponse(
                            VendorCreditResponse vendorCreditResponse) {
                    }

                    public void processUpdateSubscriptionResponse(
                            UpdateSubscriptionResponse updateSubscriptionResponse) {
                    }

                    public void processUpdatePlanResponse(
                            UpdatePlanResponse updatePlanResponse) {
                    }

                    public void processUpdateCardValidationNumOnTokenResponse(
                            UpdateCardValidationNumOnTokenResponse updateCardValidationNumOnTokenResponse) {
                    }

                    public void processUnloadResponse(
                            UnloadResponse unloadResponse) {
                    }

                    public void processSubmerchantDebitResponse(
                            SubmerchantDebitResponse submerchantDebitResponse) {
                    }

                    public void processSubmerchantCreditResponse(
                            SubmerchantCreditResponse submerchantCreditResponse) {
                    }

                    public void processSaleResponse(SaleResponse saleResponse) {
                    }

                    public void processReserveDebitResponse(
                            ReserveDebitResponse reserveDebitResponse) {
                    }

                    public void processReserveCreditResponse(
                            ReserveCreditResponse reserveCreditResponse) {
                    }

                    public void processRegisterTokenResponse(
                            RegisterTokenResponse registerTokenResponse) {
                    }

                    public void processPhysicalCheckDebitResponse(
                            PhysicalCheckDebitResponse checkDebitResponse) {
                    }

                    public void processPhysicalCheckCreditResponse(
                            PhysicalCheckCreditResponse checkCreditResponse) {
                    }

                    public void processPayFacDebitResponse(
                            PayFacDebitResponse payFacDebitResponse) {
                    }

                    public void processPayFacCreditResponse(
                            PayFacCreditResponse payFacCreditResponse) {
                    }

                    public void processLoadResponse(LoadResponse loadResponse) {
                    }

                    public void processForceCaptureResponse(
                            ForceCaptureResponse forceCaptureResponse) {
                    }

                    public void processEcheckVerificationResponse(
                            EcheckVerificationResponse echeckVerificationResponse) {
                    }

                    public void processEcheckSalesResponse(
                            EcheckSalesResponse echeckSalesResponse) {
                    }

                    public void processEcheckRedepositResponse(
                            EcheckRedepositResponse echeckRedepositResponse) {
                    }

                    public void processEcheckPreNoteSaleResponse(
                            EcheckPreNoteSaleResponse echeckPreNoteSaleResponse) {

                    }

                    public void processEcheckPreNoteCreditResponse(
                            EcheckPreNoteCreditResponse echeckPreNoteCreditResponse) {

                    }

                    public void processEcheckCreditResponse(
                            EcheckCreditResponse echeckCreditResponse) {
                    }

                    public void processDeactivateResponse(
                            DeactivateResponse deactivateResponse) {
                    }

                    public void processCreditResponse(
                            CreditResponse creditResponse) {
                    }

                    public void processCreatePlanResponse(
                            CreatePlanResponse createPlanResponse) {
                    }

                    public void processCaptureResponse(
                            CaptureResponse captureResponse) {
                    }

                    public void processCaptureGivenAuthResponse(
                            CaptureGivenAuthResponse captureGivenAuthResponse) {
                    }

                    public void processCancelSubscriptionResponse(
                            CancelSubscriptionResponse cancelSubscriptionResponse) {
                    }

                    public void processBalanceInquiryResponse(
                            BalanceInquiryResponse balanceInquiryResponse) {
                    }

                    public void processAuthorizationResponse(
                            AuthorizationResponse authorizationResponse) {
                    }

                    public void processAuthReversalResponse(
                            AuthReversalResponse authReversalResponse) {
                    }

                    public void processActivateResponse(
                            ActivateResponse activateResponse) {
                    }

                    public void processAccountUpdate(
                            AccountUpdateResponse accountUpdateResponse) {
                    }

                    public void processFundingInstructionVoidResponse(
                            FundingInstructionVoidResponse fundingInstructionVoidResponse) {

                    }

					public void processGiftCardAuthReversalResponse(
					        GiftCardAuthReversalResponse giftCardAuthReversalResponse) {
					}

					public void processGiftCardCaptureResponse(GiftCardCaptureResponse giftCardCaptureResponse) {
					}

					public void processGiftCardCreditResponse(GiftCardCreditResponse giftCardCreditResponse) {
					}

                    public void processFastAccessFundingResponse(FastAccessFundingResponse fastAccessFundingResponse) {

                    }

                    public void processTranslateToLowValueTokenResponse(TranslateToLowValueTokenResponse translateToLowValueTokenResponse) {
                    }
                })) {

            txns++;
        }

        assertEquals(transactionCount, txns);
    }

//    @Test
//    public void testPFIFInstructionTxn() {
//        String requestFileName = "cnpSdk-testBatchFile-PFIF-" + TIME_STAMP + ".xml";
//        CnpBatchFileRequest request = new CnpBatchFileRequest(
//                requestFileName);
//
//        Properties configFromFile = request.getConfig();
//
//        // pre-assert the config file has required param values
//        assertEquals("prelive.litle.com",
//                configFromFile.getProperty("batchHost"));
//        //assertEquals("15000", configFromFile.getProperty("batchPort"));
//
//        CnpBatchRequest batch = request.createBatch(configFromFile.getProperty("merchantId"));
//
//        // echeck
//        EcheckType echeck = new EcheckType();
//        echeck.setAccNum("1092969901");
//        echeck.setAccType(EcheckAccountTypeEnum.CORPORATE);
//        echeck.setRoutingNum("011075150");
//        echeck.setCheckNum("123455");
//
//        // billto address
//        Contact contact = new Contact();
//        contact.setName("Bob");
//        contact.setCity("Lowell");
//        contact.setState("MA");
//        contact.setEmail("Bob@cnp.com");
//
//        SubmerchantCredit submerchantCredit = new SubmerchantCredit();
//        submerchantCredit.setReportGroup("Planets");
//        submerchantCredit.setFundingSubmerchantId("12345");
//        submerchantCredit.setSubmerchantName("submerchant co.");
//        submerchantCredit.setFundsTransferId("000");
//        submerchantCredit.setAmount(1000L);
//        submerchantCredit.setAccountInfo(echeck);
//        submerchantCredit.setId("ID");
//        submerchantCredit.setCustomIdentifier("custom field");
//        batch.addTransaction(submerchantCredit);
//
//        PayFacCredit payFacCredit = new PayFacCredit();
//        payFacCredit.setReportGroup("Planets");
//        payFacCredit.setFundingSubmerchantId("12346");
//        payFacCredit.setFundsTransferId("000");
//        payFacCredit.setAmount(1000L);
//        payFacCredit.setId("ID");
//        batch.addTransaction(payFacCredit);
//
//        VendorCredit vendorCredit = new VendorCredit();
//        vendorCredit.setReportGroup("Planets");
//        vendorCredit.setFundingSubmerchantId("12347");
//        vendorCredit.setVendorName("vendor co.");
//        vendorCredit.setFundsTransferId("000");
//        vendorCredit.setAmount(1000L);
//        vendorCredit.setAccountInfo(echeck);
//        vendorCredit.setId("ID");
//        batch.addTransaction(vendorCredit);
//
//        ReserveCredit reserveCredit = new ReserveCredit();
//        reserveCredit.setReportGroup("Planets");
//        reserveCredit.setFundingSubmerchantId("12348");
//        reserveCredit.setFundsTransferId("000");
//        reserveCredit.setAmount(1000L);
//        reserveCredit.setId("ID");
//        batch.addTransaction(reserveCredit);
//
//        PhysicalCheckCredit physicalCheckCredit = new PhysicalCheckCredit();
//        physicalCheckCredit.setReportGroup("Planets");
//        physicalCheckCredit.setFundingSubmerchantId("12349");
//        physicalCheckCredit.setFundsTransferId("000");
//        physicalCheckCredit.setAmount(1000L);
//        physicalCheckCredit.setId("ID");
//        batch.addTransaction(physicalCheckCredit);
//
//        SubmerchantDebit submerchantDebit = new SubmerchantDebit();
//        submerchantDebit.setReportGroup("Planets");
//        submerchantDebit.setFundingSubmerchantId("12345");
//        submerchantDebit.setSubmerchantName("submerchant co.");
//        submerchantDebit.setFundsTransferId("000");
//        submerchantDebit.setAmount(1000L);
//        submerchantDebit.setAccountInfo(echeck);
//        submerchantDebit.setId("ID");
//        submerchantDebit.setCustomIdentifier("custom field");
//        batch.addTransaction(submerchantDebit);
//
//        PayFacDebit payFacDebit = new PayFacDebit();
//        payFacDebit.setReportGroup("Planets");
//        payFacDebit.setFundingSubmerchantId("12346");
//        payFacDebit.setFundsTransferId("000");
//        payFacDebit.setAmount(1000L);
//        payFacDebit.setId("ID");
//        batch.addTransaction(payFacDebit);
//
//        VendorDebit vendorDebit = new VendorDebit();
//        vendorDebit.setReportGroup("Planets");
//        vendorDebit.setFundingSubmerchantId("12347");
//        vendorDebit.setVendorName("vendor co.");
//        vendorDebit.setFundsTransferId("000");
//        vendorDebit.setAmount(1000L);
//        vendorDebit.setAccountInfo(echeck);
//        vendorDebit.setId("ID");
//        batch.addTransaction(vendorDebit);
//
//        ReserveDebit reserveDebit = new ReserveDebit();
//        reserveDebit.setReportGroup("Planets");
//        reserveDebit.setFundingSubmerchantId("12348");
//        reserveDebit.setFundsTransferId("000");
//        reserveDebit.setAmount(1000L);
//        reserveDebit.setId("ID");
//        batch.addTransaction(reserveDebit);
//
//        PhysicalCheckDebit physicalCheckDebit = new PhysicalCheckDebit();
//        physicalCheckDebit.setReportGroup("Planets");
//        physicalCheckDebit.setFundingSubmerchantId("12349");
//        physicalCheckDebit.setFundsTransferId("000");
//        physicalCheckDebit.setAmount(1000L);
//        physicalCheckDebit.setId("ID");
//        batch.addTransaction(physicalCheckDebit);
//
//        int transactionCount = batch.getNumberOfTransactions();
//
//        CnpBatchFileResponse fileResponse = request.sendToCnpSFTP();
//        CnpBatchResponse batchResponse = fileResponse
//                .getNextCnpBatchResponse();
//        int txns = 0;
//
//        ResponseValidatorProcessor processor = new ResponseValidatorProcessor();
//
//        while (batchResponse.processNextTransaction(processor)) {
//            txns++;
//        }
//
//        assertEquals(transactionCount, txns);
//        assertEquals(transactionCount, processor.responseCount);
//    }

    @Test
    public void testGiftCardTransactions() {
        String requestFileName = "cnpSdk-testBatchFile-GiftCardTransactions-" + TIME_STAMP + ".xml";
        CnpBatchFileRequest request = new CnpBatchFileRequest(
                requestFileName);

        Properties configFromFile = request.getConfig();

        // pre-assert the config file has required param values
        assertEquals("payments.vantivprelive.com",
                configFromFile.getProperty("batchHost"));
       // assertEquals("15000", configFromFile.getProperty("batchPort"));

        CnpBatchRequest batch = request.createBatch(configFromFile.getProperty("merchantId"));

        CardType card = new CardType();
        card.setType(MethodOfPaymentTypeEnum.GC);
        card.setExpDate("1218");
        card.setNumber("4100000000000001");

        VirtualGiftCardType virtualGiftCard = new VirtualGiftCardType();
        virtualGiftCard.setGiftCardBin("abcd1234");

        GiftCardCardType giftCard = new GiftCardCardType();
        giftCard.setType(MethodOfPaymentTypeEnum.GC);
        giftCard.setNumber("4100000000000001");
        giftCard.setExpDate("0850");
        giftCard.setCardValidationNum("111");
        giftCard.setPin("4111");

        Activate activate = new Activate();
        activate.setReportGroup("Planets");
        activate.setOrderSource(OrderSourceType.ECOMMERCE);
        activate.setAmount(100L);
        activate.setOrderId("abc");
        activate.setCard(giftCard);
        activate.setId("id");
        batch.addTransaction(activate);

        Deactivate deactivate = new Deactivate();
        deactivate.setReportGroup("Planets");
        deactivate.setOrderId("def");
        deactivate.setOrderSource(OrderSourceType.ECOMMERCE);
        deactivate.setCard(giftCard);
        deactivate.setId("id");
        batch.addTransaction(deactivate);

        Load load = new Load();
        load.setReportGroup("Planets");
        load.setOrderId("ghi");
        load.setAmount(100L);
        load.setOrderSource(OrderSourceType.ECOMMERCE);
        load.setCard(giftCard);
        load.setId("id");
        batch.addTransaction(load);

        Unload unload = new Unload();
        unload.setReportGroup("Planets");
        unload.setOrderId("jkl");
        unload.setAmount(100L);
        unload.setOrderSource(OrderSourceType.ECOMMERCE);
        unload.setCard(giftCard);
        unload.setId("id");
        batch.addTransaction(unload);

        BalanceInquiry balanceInquiry = new BalanceInquiry();
        balanceInquiry.setReportGroup("Planets");
        balanceInquiry.setOrderId("mno");
        balanceInquiry.setOrderSource(OrderSourceType.ECOMMERCE);
        balanceInquiry.setCard(giftCard);
        balanceInquiry.setId("id");
        batch.addTransaction(balanceInquiry);

        GiftCardAuthReversal gcAuthReversal = new GiftCardAuthReversal();
        gcAuthReversal.setId("979797");
        gcAuthReversal.setCustomerId("customer_23");
        gcAuthReversal.setCnpTxnId(8521478963210145l);
        gcAuthReversal.setReportGroup("rptGrp2");
        gcAuthReversal.setOriginalAmount(45l);
        gcAuthReversal.setOriginalSequenceNumber("333333");
        gcAuthReversal.setOriginalTxnTime(new XMLGregorianCalendarImpl(new GregorianCalendar()));
        gcAuthReversal.setOriginalSystemTraceId(0);
        gcAuthReversal.setOriginalRefCode("ref");
        gcAuthReversal.setCard(giftCard);
        batch.addTransaction(gcAuthReversal);

        GiftCardCapture gcCapture = new GiftCardCapture();
        gcCapture.setCnpTxnId(123L);
        gcCapture.setId("id");
        gcCapture.setReportGroup("rptGrp");
        gcCapture.setCaptureAmount(2434l);
        gcCapture.setCard(giftCard);
        gcCapture.setOriginalRefCode("ref");
        gcCapture.setOriginalAmount(44455l);
        gcCapture.setOriginalTxnTime(new XMLGregorianCalendarImpl(new GregorianCalendar()));
        batch.addTransaction(gcCapture);

        GiftCardCredit gcCredit = new GiftCardCredit();
        gcCredit.setCnpTxnId(369852147l);
        gcCredit.setId("id");
        gcCredit.setReportGroup("rptGrp1");
        gcCredit.setCustomerId("customer_22");
        gcCredit.setCreditAmount(1942l);
        gcCredit.setCard(giftCard);
        batch.addTransaction(gcCredit);

        int transactionCount = batch.getNumberOfTransactions();
        CnpBatchFileResponse fileResponse = request.sendToCnpSFTP();
        CnpBatchResponse batchResponse = fileResponse.getNextCnpBatchResponse();
        int txns = 0;
        // iterate over all transactions in the file with a custom response
        // processor
        while (batchResponse
                .processNextTransaction(new CnpResponseProcessor() {
                    public void processAuthorizationResponse(
                            AuthorizationResponse authorizationResponse) {
                        assertNotNull(authorizationResponse.getCnpTxnId());
                    }

                    public void processCaptureResponse(
                            CaptureResponse captureResponse) {
                        assertNotNull(captureResponse.getCnpTxnId());
                    }

                    public void processForceCaptureResponse(
                            ForceCaptureResponse forceCaptureResponse) {
                        assertNotNull(forceCaptureResponse.getCnpTxnId());
                    }

                    public void processCaptureGivenAuthResponse(
                            CaptureGivenAuthResponse captureGivenAuthResponse) {
                        assertNotNull(captureGivenAuthResponse.getCnpTxnId());
                    }

                    public void processSaleResponse(SaleResponse saleResponse) {
                        assertNotNull(saleResponse.getCnpTxnId());
                    }

                    public void processCreditResponse(
                            CreditResponse creditResponse) {
                        assertNotNull(creditResponse.getCnpTxnId());
                    }

                    public void processEcheckSalesResponse(
                            EcheckSalesResponse echeckSalesResponse) {
                        assertNotNull(echeckSalesResponse.getCnpTxnId());
                    }

                    public void processEcheckCreditResponse(
                            EcheckCreditResponse echeckCreditResponse) {
                        assertNotNull(echeckCreditResponse.getCnpTxnId());
                    }

                    public void processEcheckVerificationResponse(
                            EcheckVerificationResponse echeckVerificationResponse) {
                        assertNotNull(echeckVerificationResponse.getCnpTxnId());
                    }

                    public void processEcheckRedepositResponse(
                            EcheckRedepositResponse echeckRedepositResponse) {
                        assertNotNull(echeckRedepositResponse.getCnpTxnId());
                    }

                    public void processAuthReversalResponse(
                            AuthReversalResponse authReversalResponse) {
                        assertNotNull(authReversalResponse.getCnpTxnId());
                    }

                    public void processRegisterTokenResponse(
                            RegisterTokenResponse registerTokenResponse) {
                        assertNotNull(registerTokenResponse.getCnpTxnId());
                    }

                    public void processUpdateSubscriptionResponse(
                            UpdateSubscriptionResponse updateSubscriptionResponse) {
                        assertNotNull(updateSubscriptionResponse.getCnpTxnId());
                    }

                    public void processCancelSubscriptionResponse(
                            CancelSubscriptionResponse cancelSubscriptionResponse) {
                        assertNotNull(cancelSubscriptionResponse.getCnpTxnId());
                    }

                    public void processUpdateCardValidationNumOnTokenResponse(
                            UpdateCardValidationNumOnTokenResponse updateCardValidationNumOnTokenResponse) {
                        assertNotNull(updateCardValidationNumOnTokenResponse.getCnpTxnId());
                    }

                    public void processAccountUpdate(
                            AccountUpdateResponse accountUpdateResponse) {
                        assertNotNull(accountUpdateResponse.getCnpTxnId());
                    }

                    public void processCreatePlanResponse(
                            CreatePlanResponse createPlanResponse) {
                        assertNotNull(createPlanResponse.getCnpTxnId());
                    }

                    public void processUpdatePlanResponse(
                            UpdatePlanResponse updatePlanResponse) {
                        assertNotNull(updatePlanResponse.getCnpTxnId());
                    }

                    public void processActivateResponse(
                            ActivateResponse activateResponse) {
                        assertNotNull(activateResponse.getCnpTxnId());
                    }

                    public void processDeactivateResponse(
                            DeactivateResponse deactivateResponse) {
                        assertNotNull(deactivateResponse.getCnpTxnId());
                    }

                    public void processLoadResponse(LoadResponse loadResponse) {
                        assertNotNull(loadResponse.getCnpTxnId());
                    }

                    public void processUnloadResponse(
                            UnloadResponse unloadResponse) {
                        assertNotNull(unloadResponse.getCnpTxnId());
                    }

                    public void processBalanceInquiryResponse(
                            BalanceInquiryResponse balanceInquiryResponse) {
                        assertNotNull(balanceInquiryResponse.getCnpTxnId());
                    }

                    public void processEcheckPreNoteSaleResponse(
                            EcheckPreNoteSaleResponse echeckPreNoteSaleResponse) {
                    }

                    public void processEcheckPreNoteCreditResponse(
                            EcheckPreNoteCreditResponse echeckPreNoteCreditResponse) {
                    }

                    public void processSubmerchantCreditResponse(
                            SubmerchantCreditResponse submerchantCreditResponse) {
                    }

                    public void processPayFacCreditResponse(
                            PayFacCreditResponse payFacCreditResponse) {
                    }

                    public void processVendorCreditResponse(
                            VendorCreditResponse vendorCreditResponse) {
                    }

                    public void processReserveCreditResponse(
                            ReserveCreditResponse reserveCreditResponse) {
                    }

                    public void processPhysicalCheckCreditResponse(
                            PhysicalCheckCreditResponse checkCreditResponse) {
                    }

                    public void processSubmerchantDebitResponse(
                            SubmerchantDebitResponse submerchantDebitResponse) {
                    }

                    public void processPayFacDebitResponse(
                            PayFacDebitResponse payFacDebitResponse) {
                    }

                    public void processVendorDebitResponse(
                            VendorDebitResponse vendorDebitResponse) {
                    }

                    public void processReserveDebitResponse(
                            ReserveDebitResponse reserveDebitResponse) {
                    }

                    public void processPhysicalCheckDebitResponse(
                            PhysicalCheckDebitResponse checkDebitResponse) {
                    }

                    public void processFundingInstructionVoidResponse(
                            FundingInstructionVoidResponse fundingInstructionVoidResponse) {
                    }

					public void processGiftCardAuthReversalResponse(GiftCardAuthReversalResponse giftCardAuthReversalResponse) {
                        assertNotNull(giftCardAuthReversalResponse.getCnpTxnId());
					}

					public void processGiftCardCaptureResponse(GiftCardCaptureResponse giftCardCaptureResponse) {
                        assertNotNull(giftCardCaptureResponse.getCnpTxnId());
					}

					public void processGiftCardCreditResponse(GiftCardCreditResponse giftCardCreditResponse) {
                        assertNotNull(giftCardCreditResponse.getCnpTxnId());
					}


                    public void processFastAccessFundingResponse(FastAccessFundingResponse fastAccessFundingResponse) {
                    }

                    public void processTranslateToLowValueTokenResponse(TranslateToLowValueTokenResponse translateToLowValueTokenResponse) {
                    }
                })) {
            txns++;
        }

        assertEquals(5, txns);
    }

    @Test
    public void testMechaBatchAndProcess_RecurringDemonstratesUseOfProcessorAdapter() {
        String requestFileName = "cnpSdk-testBatchFile-RECURRING-" + TIME_STAMP + ".xml";
        CnpBatchFileRequest request = new CnpBatchFileRequest(
                requestFileName);

        Properties configFromFile = request.getConfig();

        // pre-assert the config file has required param values
        assertEquals("payments.vantivprelive.com",
                configFromFile.getProperty("batchHost"));
       // assertEquals("15000", configFromFile.getProperty("batchPort"));

        CnpBatchRequest batch = request.createBatch(configFromFile.getProperty("merchantId"));
        CancelSubscription cancelSubscription = new CancelSubscription();
        cancelSubscription.setSubscriptionId(12345L);
        batch.addTransaction(cancelSubscription);

        UpdateSubscription updateSubscription = new UpdateSubscription();
        updateSubscription.setSubscriptionId(12345L);
        batch.addTransaction(updateSubscription);

        CreatePlan createPlan = new CreatePlan();
        createPlan.setPlanCode("abc");
        createPlan.setName("name");
        createPlan.setIntervalType(IntervalTypeEnum.ANNUAL);
        createPlan.setAmount(100L);
        batch.addTransaction(createPlan);

        UpdatePlan updatePlan = new UpdatePlan();
        updatePlan.setPlanCode("def");
        updatePlan.setActive(true);
        batch.addTransaction(updatePlan);

        CnpBatchFileResponse fileResponse = request.sendToCnpSFTP();
        CnpBatchResponse batchResponse = fileResponse
                .getNextCnpBatchResponse();
        int txns = 0;
        // iterate over all transactions in the file with a custom response
        // processor
        while (batchResponse
                .processNextTransaction(new CnpResponseProcessorAdapter() {
                    @Override
                    public void processUpdateSubscriptionResponse(
                            UpdateSubscriptionResponse updateSubscriptionResponse) {
                        assertEquals(12345L,
                                updateSubscriptionResponse.getSubscriptionId());
                    }

                    @Override
                    public void processCancelSubscriptionResponse(
                            CancelSubscriptionResponse cancelSubscriptionResponse) {
                        assertEquals(12345L,
                                cancelSubscriptionResponse.getSubscriptionId());
                    }

                    @Override
                    public void processCreatePlanResponse(
                            CreatePlanResponse createPlanResponse) {
                        assertEquals("abc", createPlanResponse.getPlanCode());
                    }

                    @Override
                    public void processUpdatePlanResponse(
                            UpdatePlanResponse updatePlanResponse) {
                        assertEquals("def", updatePlanResponse.getPlanCode());
                    }
                })) {
            txns++;
        }

        assertEquals(4, txns);
    }

    @Test
    public void testBatch_AU() {
        String requestFileName = "cnpSdk-testBatchFile_AU-" + TIME_STAMP + ".xml";
        CnpBatchFileRequest request = new CnpBatchFileRequest(
                requestFileName);

        Properties configFromFile = request.getConfig();

        // pre-assert the config file has required param values
        assertEquals("payments.vantivprelive.com",
                configFromFile.getProperty("batchHost"));
       // assertEquals("15000", configFromFile.getProperty("batchPort"));

        CnpBatchRequest batch = request.createBatch(configFromFile.getProperty("merchantId"));

        // card
        CardType card = new CardType();
        card.setNumber("4100000000000001");
        card.setExpDate("1210");
        card.setType(MethodOfPaymentTypeEnum.VI);

        ObjectFactory objectFactory = new ObjectFactory();
        AccountUpdate accountUpdate = new AccountUpdate();
        accountUpdate.setReportGroup("Planets");
        accountUpdate.setId("12345");
        accountUpdate.setCustomerId("0987");
        accountUpdate.setOrderId("1234");
        accountUpdate.setCardOrToken(objectFactory.createCard(card));

        batch.addTransaction(accountUpdate);

        CnpBatchFileResponse fileResponse = request.sendToCnpSFTP();
        CnpBatchResponse batchResponse = fileResponse.getNextCnpBatchResponse();
        int txns = 0;
        // iterate over all transactions in the file with a custom response
        // processor
        while (batchResponse
                .processNextTransaction(new CnpResponseProcessor() {
                    public void processAuthorizationResponse(
                            AuthorizationResponse authorizationResponse) {

                    }

                    public void processCaptureResponse(
                            CaptureResponse captureResponse) {
                    }
                    
                    public void processForceCaptureResponse(
                            ForceCaptureResponse forceCaptureResponse) {
                    }

                    public void processCaptureGivenAuthResponse(
                            CaptureGivenAuthResponse captureGivenAuthResponse) {
                    }

                    public void processSaleResponse(SaleResponse saleResponse) {
                    }

                    public void processCreditResponse(
                            CreditResponse creditResponse) {
                    }

                    public void processEcheckSalesResponse(
                            EcheckSalesResponse echeckSalesResponse) {
                    }

                    public void processEcheckCreditResponse(
                            EcheckCreditResponse echeckCreditResponse) {
                    }

                    public void processEcheckVerificationResponse(
                            EcheckVerificationResponse echeckVerificationResponse) {
                    }

                    public void processEcheckRedepositResponse(
                            EcheckRedepositResponse echeckRedepositResponse) {
                    }

                    public void processAuthReversalResponse(
                            AuthReversalResponse authReversalResponse) {
                    }

                    public void processRegisterTokenResponse(
                            RegisterTokenResponse registerTokenResponse) {
                    }

                    public void processAccountUpdate(
                            AccountUpdateResponse accountUpdateResponse) {
                        assertEquals("Planets",
                                accountUpdateResponse.getReportGroup());
                        assertEquals("12345", accountUpdateResponse.getId());
                        assertEquals("0987",
                                accountUpdateResponse.getCustomerId());
                    }

                    public void processUpdateSubscriptionResponse(
                            UpdateSubscriptionResponse updateSubscriptionResponse) {
                    }

                    public void processCancelSubscriptionResponse(
                            CancelSubscriptionResponse cancelSubscriptionResponse) {
                    }

                    public void processUpdateCardValidationNumOnTokenResponse(
                            UpdateCardValidationNumOnTokenResponse updateCardValidationNumOnTokenResponse) {
                    }

                    public void processCreatePlanResponse(
                            CreatePlanResponse createPlanResponse) {
                    }

                    public void processUpdatePlanResponse(
                            UpdatePlanResponse updatePlanResponse) {
                    }

                    public void processActivateResponse(
                            ActivateResponse activateResponse) {
                    }

                    public void processDeactivateResponse(
                            DeactivateResponse deactivateResponse) {
                    }

                    public void processLoadResponse(LoadResponse loadResponse) {
                    }

                    public void processUnloadResponse(
                            UnloadResponse unloadResponse) {
                    }

                    public void processBalanceInquiryResponse(
                            BalanceInquiryResponse balanceInquiryResponse) {
                    }

                    public void processEcheckPreNoteSaleResponse(
                            EcheckPreNoteSaleResponse echeckPreNoteSaleResponse) {
                    }

                    public void processEcheckPreNoteCreditResponse(
                            EcheckPreNoteCreditResponse echeckPreNoteCreditResponse) {
                    }

                    public void processSubmerchantCreditResponse(
                            SubmerchantCreditResponse submerchantCreditResponse) {
                    }

                    public void processPayFacCreditResponse(
                            PayFacCreditResponse payFacCreditResponse) {
                    }

                    public void processVendorCreditResponse(
                            VendorCreditResponse vendorCreditResponse) {
                    }

                    public void processReserveCreditResponse(
                            ReserveCreditResponse reserveCreditResponse) {
                    }

                    public void processPhysicalCheckCreditResponse(
                            PhysicalCheckCreditResponse checkCreditResponse) {
                    }

                    public void processSubmerchantDebitResponse(
                            SubmerchantDebitResponse submerchantDebitResponse) {
                    }

                    public void processPayFacDebitResponse(
                            PayFacDebitResponse payFacDebitResponse) {
                    }

                    public void processVendorDebitResponse(
                            VendorDebitResponse vendorDebitResponse) {
                    }

                    public void processReserveDebitResponse(
                            ReserveDebitResponse reserveDebitResponse) {
                    }

                    public void processPhysicalCheckDebitResponse(
                            PhysicalCheckDebitResponse checkDebitResponse) {
                    }

                    public void processFundingInstructionVoidResponse(
                            FundingInstructionVoidResponse fundingInstructionVoidResponse) {
                    }

					public void processGiftCardAuthReversalResponse(GiftCardAuthReversalResponse giftCardAuthReversalResponse) {
					}

					public void processGiftCardCaptureResponse(GiftCardCaptureResponse giftCardCaptureResponse) {
					}

					public void processGiftCardCreditResponse(GiftCardCreditResponse giftCardCreditResponse) {
					}

                    public void processFastAccessFundingResponse(FastAccessFundingResponse fastAccessFundingResponse) {
                    }

                    public void processTranslateToLowValueTokenResponse(TranslateToLowValueTokenResponse translateToLowValueTokenResponse){
                    }
                })) {
            txns++;
        }

        assertEquals(1, txns);
    }



    @Test
    public void testCtxAll(){
        String requestFileName = "cnpSdk-testBatchFile-CtxAll-" + TIME_STAMP + ".xml";
        CnpBatchFileRequest request = new CnpBatchFileRequest(
                requestFileName);

        Properties configFromFile = request.getConfig();

        configFromFile.setProperty("merchantId", configFromFile.getProperty("merchantId"));
        configFromFile.setProperty("username", configFromFile.getProperty("username"));
        configFromFile.setProperty("sftpUsername", configFromFile.getProperty("sftpUsername"));
        configFromFile.setProperty("password", configFromFile.getProperty("password"));
        configFromFile.setProperty("sftpPassword", configFromFile.getProperty("sftpPassword"));

        // pre-assert the config file has required param values
        assertEquals("payments.vantivprelive.com",
                configFromFile.getProperty("batchHost"));
        //assertEquals("15000", configFromFile.getProperty("batchPort"));

        CnpBatchRequest batch = request.createBatch(configFromFile.getProperty("merchantId"));

        String fundsTransferIdString = ""+System.currentTimeMillis();

        EcheckTypeCtx echeck = new EcheckTypeCtx();
        echeck.setAccNum("1092969901");
        echeck.setAccType(EcheckAccountTypeEnum.CHECKING);
        echeck.setRoutingNum("011075150");
        echeck.setCheckNum("123455");

        CtxPaymentInformationType echeckPayInfo = new CtxPaymentInformationType();
        List<String> payDetails = echeckPayInfo.getCtxPaymentDetails();
        payDetails.add("ctx1 for submerchantcredit");
        echeck.setCtxPaymentInformation(echeckPayInfo);

        VendorCreditCtx vcredit = new VendorCreditCtx();
        vcredit.setReportGroup("vendorCredit");
        vcredit.setId("111");
        vcredit.setFundingSubmerchantId("vendorCreditTest");
        vcredit.setVendorName("Vendor101");
        vcredit.setFundsTransferId(fundsTransferIdString);
        vcredit.setAmount(500l);
        vcredit.setAccountInfo(echeck);
        batch.addTransaction(vcredit);

        VendorDebitCtx vdebit = new VendorDebitCtx();
        vdebit.setReportGroup("vendorDebit");
        vdebit.setId("111");
        vdebit.setFundingSubmerchantId("vendorDebit");
        vdebit.setVendorName("Vendor101");
        vdebit.setFundsTransferId(fundsTransferIdString);
        vdebit.setAmount(500l);
        vdebit.setAccountInfo(echeck);
        batch.addTransaction(vdebit);

        SubmerchantCreditCtx smcredit = new SubmerchantCreditCtx();
        smcredit.setReportGroup("submerchantCredit");
        smcredit.setId("111");
        smcredit.setFundingSubmerchantId("submerchantCredit");
        smcredit.setSubmerchantName("submerchant101");
        smcredit.setFundsTransferId(fundsTransferIdString);
        smcredit.setAmount(500l);
        smcredit.setAccountInfo(echeck);
        batch.addTransaction(smcredit);

        SubmerchantDebitCtx smdebit = new SubmerchantDebitCtx();
        smdebit.setReportGroup("submerchantDebit");
        smdebit.setId("111");
        smdebit.setFundingSubmerchantId("submerchantDebit");
        smdebit.setSubmerchantName("submerchant101");
        smdebit.setFundsTransferId(fundsTransferIdString);
        smdebit.setAmount(500l);
        smdebit.setAccountInfo(echeck);
        batch.addTransaction(smdebit);

        int transactionCount = batch.getNumberOfTransactions();

        CnpBatchFileResponse fileResponse = request.sendToCnpSFTP();
        CnpBatchResponse batchResponse = fileResponse
                .getNextCnpBatchResponse();
        int txns = 0;

//        ResponseValidatorProcessor processor = new ResponseValidatorProcessor();

        while (batchResponse
                .processNextTransaction(new CnpResponseProcessor() {

                    public void processVendorDebitResponse(
                            VendorDebitResponse vendorDebitResponse) {
                    }

                    public void processVendorCreditResponse(
                            VendorCreditResponse vendorCreditResponse) {
                    }

                    public void processUpdateSubscriptionResponse(
                            UpdateSubscriptionResponse updateSubscriptionResponse) {
                    }

                    public void processUpdatePlanResponse(
                            UpdatePlanResponse updatePlanResponse) {
                    }

                    public void processUpdateCardValidationNumOnTokenResponse(
                            UpdateCardValidationNumOnTokenResponse updateCardValidationNumOnTokenResponse) {
                    }

                    public void processUnloadResponse(
                            UnloadResponse unloadResponse) {
                    }

                    public void processSubmerchantDebitResponse(
                            SubmerchantDebitResponse submerchantDebitResponse) {
                    }

                    public void processSubmerchantCreditResponse(
                            SubmerchantCreditResponse submerchantCreditResponse) {
                    }

                    public void processSaleResponse(SaleResponse saleResponse) {
                    }

                    public void processReserveDebitResponse(
                            ReserveDebitResponse reserveDebitResponse) {
                    }

                    public void processReserveCreditResponse(
                            ReserveCreditResponse reserveCreditResponse) {
                    }

                    public void processRegisterTokenResponse(
                            RegisterTokenResponse registerTokenResponse) {
                    }

                    public void processPhysicalCheckDebitResponse(
                            PhysicalCheckDebitResponse checkDebitResponse) {
                    }

                    public void processPhysicalCheckCreditResponse(
                            PhysicalCheckCreditResponse checkCreditResponse) {
                    }

                    public void processPayFacDebitResponse(
                            PayFacDebitResponse payFacDebitResponse) {
                    }

                    public void processPayFacCreditResponse(
                            PayFacCreditResponse payFacCreditResponse) {
                    }

                    public void processLoadResponse(LoadResponse loadResponse) {
                    }

                    public void processForceCaptureResponse(
                            ForceCaptureResponse forceCaptureResponse) {
                    }

                    public void processEcheckVerificationResponse(
                            EcheckVerificationResponse echeckVerificationResponse) {
                    }

                    public void processEcheckSalesResponse(
                            EcheckSalesResponse echeckSalesResponse) {
                    }

                    public void processEcheckRedepositResponse(
                            EcheckRedepositResponse echeckRedepositResponse) {
                    }

                    public void processEcheckPreNoteSaleResponse(
                            EcheckPreNoteSaleResponse echeckPreNoteSaleResponse) {

                    }

                    public void processEcheckPreNoteCreditResponse(
                            EcheckPreNoteCreditResponse echeckPreNoteCreditResponse) {

                    }

                    public void processEcheckCreditResponse(
                            EcheckCreditResponse echeckCreditResponse) {
                    }

                    public void processDeactivateResponse(
                            DeactivateResponse deactivateResponse) {
                    }

                    public void processCreditResponse(
                            CreditResponse creditResponse) {
                    }

                    public void processCreatePlanResponse(
                            CreatePlanResponse createPlanResponse) {
                    }

                    public void processCaptureResponse(
                            CaptureResponse captureResponse) {
                    }

                    public void processCaptureGivenAuthResponse(
                            CaptureGivenAuthResponse captureGivenAuthResponse) {
                    }

                    public void processCancelSubscriptionResponse(
                            CancelSubscriptionResponse cancelSubscriptionResponse) {
                    }

                    public void processBalanceInquiryResponse(
                            BalanceInquiryResponse balanceInquiryResponse) {
                    }

                    public void processAuthorizationResponse(
                            AuthorizationResponse authorizationResponse) {
                    }

                    public void processAuthReversalResponse(
                            AuthReversalResponse authReversalResponse) {
                    }

                    public void processActivateResponse(
                            ActivateResponse activateResponse) {
                    }

                    public void processAccountUpdate(
                            AccountUpdateResponse accountUpdateResponse) {
                    }

                    public void processFundingInstructionVoidResponse(
                            FundingInstructionVoidResponse fundingInstructionVoidResponse) {

                    }

                    public void processGiftCardAuthReversalResponse(
                            GiftCardAuthReversalResponse giftCardAuthReversalResponse) {
                    }

                    public void processGiftCardCaptureResponse(GiftCardCaptureResponse giftCardCaptureResponse) {
                    }

                    public void processGiftCardCreditResponse(GiftCardCreditResponse giftCardCreditResponse) {
                    }

                    public void processFastAccessFundingResponse(FastAccessFundingResponse fastAccessFundingResponse) {

                    }

                    public void processTranslateToLowValueTokenResponse(TranslateToLowValueTokenResponse translateToLowValueTokenResponse) {
                    }
                })) {

            txns++;
        }

        assertEquals(transactionCount, txns);

    }


//    @Test
//    public void testBatch_FastAccessFunding() {
//        String requestFileName = "cnpSdk-testBatchFile_fastAccessFunding-" + TIME_STAMP + ".xml";
//        CnpBatchFileRequest request = new CnpBatchFileRequest(
//                requestFileName);
//
//        Properties configFromFile = request.getConfig();
//
//        // pre-assert the config file has required param values
//        assertEquals("prelive.litle.com",
//                configFromFile.getProperty("batchHost"));
//        // assertEquals("15000", configFromFile.getProperty("batchPort"));
//
//        CnpBatchRequest batch = request.createBatch(configFromFile.getProperty("merchantId"));
//
//        FastAccessFunding fastAccessFunding = new FastAccessFunding();
//        fastAccessFunding.setId("id");
//        fastAccessFunding.setReportGroup("Planets");
//        fastAccessFunding.setCustomerId("0987");
//        fastAccessFunding.setFundingSubmerchantId("2111");
//        fastAccessFunding.setSubmerchantName("001");
//        fastAccessFunding.setFundsTransferId("1234567891111111");
//        fastAccessFunding.setAmount(20L);
//        CardType card = new CardType();
//        card.setType(MethodOfPaymentTypeEnum.VI);
//        card.setNumber("4457010000000009");
//        card.setExpDate("0114");
//        fastAccessFunding.setCard(card);
//
//        batch.addTransaction(fastAccessFunding);
//
//        CnpBatchFileResponse fileResponse = request.sendToCnpSFTP();
//        CnpBatchResponse batchResponse = fileResponse
//                .getNextCnpBatchResponse();
//        int txns = 0;
//        // iterate over all transactions in the file with a custom response
//        // processor
//        while (batchResponse
//                .processNextTransaction(new CnpResponseProcessor() {
//                    public void processAuthorizationResponse(
//                            AuthorizationResponse authorizationResponse) {
//
//                    }
//
//                    public void processCaptureResponse(
//                            CaptureResponse captureResponse) {
//                    }
//
//                    public void processForceCaptureResponse(
//                            ForceCaptureResponse forceCaptureResponse) {
//                    }
//
//                    public void processCaptureGivenAuthResponse(
//                            CaptureGivenAuthResponse captureGivenAuthResponse) {
//                    }
//
//                    public void processSaleResponse(SaleResponse saleResponse) {
//                    }
//
//                    public void processCreditResponse(
//                            CreditResponse creditResponse) {
//                    }
//
//                    public void processEcheckSalesResponse(
//                            EcheckSalesResponse echeckSalesResponse) {
//                    }
//
//                    public void processEcheckCreditResponse(
//                            EcheckCreditResponse echeckCreditResponse) {
//                    }
//
//                    public void processEcheckVerificationResponse(
//                            EcheckVerificationResponse echeckVerificationResponse) {
//                    }
//
//                    public void processEcheckRedepositResponse(
//                            EcheckRedepositResponse echeckRedepositResponse) {
//                    }
//
//                    public void processAuthReversalResponse(
//                            AuthReversalResponse authReversalResponse) {
//                    }
//
//                    public void processRegisterTokenResponse(
//                            RegisterTokenResponse registerTokenResponse) {
//                    }
//
//                    public void processAccountUpdate(
//                            AccountUpdateResponse accountUpdateResponse) {
//                    }
//
//                    public void processUpdateSubscriptionResponse(
//                            UpdateSubscriptionResponse updateSubscriptionResponse) {
//                    }
//
//                    public void processCancelSubscriptionResponse(
//                            CancelSubscriptionResponse cancelSubscriptionResponse) {
//                    }
//
//                    public void processUpdateCardValidationNumOnTokenResponse(
//                            UpdateCardValidationNumOnTokenResponse updateCardValidationNumOnTokenResponse) {
//                    }
//
//                    public void processCreatePlanResponse(
//                            CreatePlanResponse createPlanResponse) {
//                    }
//
//                    public void processUpdatePlanResponse(
//                            UpdatePlanResponse updatePlanResponse) {
//                    }
//
//                    public void processActivateResponse(
//                            ActivateResponse activateResponse) {
//                    }
//
//                    public void processDeactivateResponse(
//                            DeactivateResponse deactivateResponse) {
//                    }
//
//                    public void processLoadResponse(LoadResponse loadResponse) {
//                    }
//
//                    public void processUnloadResponse(
//                            UnloadResponse unloadResponse) {
//                    }
//
//                    public void processBalanceInquiryResponse(
//                            BalanceInquiryResponse balanceInquiryResponse) {
//                    }
//
//                    public void processEcheckPreNoteSaleResponse(
//                            EcheckPreNoteSaleResponse echeckPreNoteSaleResponse) {
//                    }
//
//                    public void processEcheckPreNoteCreditResponse(
//                            EcheckPreNoteCreditResponse echeckPreNoteCreditResponse) {
//                    }
//
//                    public void processSubmerchantCreditResponse(
//                            SubmerchantCreditResponse submerchantCreditResponse) {
//                    }
//
//                    public void processPayFacCreditResponse(
//                            PayFacCreditResponse payFacCreditResponse) {
//                    }
//
//                    public void processVendorCreditRespsonse(
//                            VendorCreditResponse vendorCreditResponse) {
//                    }
//
//                    public void processReserveCreditResponse(
//                            ReserveCreditResponse reserveCreditResponse) {
//                    }
//
//                    public void processPhysicalCheckCreditResponse(
//                            PhysicalCheckCreditResponse checkCreditResponse) {
//                    }
//
//                    public void processSubmerchantDebitResponse(
//                            SubmerchantDebitResponse submerchantDebitResponse) {
//                    }
//
//                    public void processPayFacDebitResponse(
//                            PayFacDebitResponse payFacDebitResponse) {
//                    }
//
//                    public void processVendorDebitResponse(
//                            VendorDebitResponse vendorDebitResponse) {
//                    }
//
//                    public void processReserveDebitResponse(
//                            ReserveDebitResponse reserveDebitResponse) {
//                    }
//
//                    public void processPhysicalCheckDebitResponse(
//                            PhysicalCheckDebitResponse checkDebitResponse) {
//                    }
//
//                    public void processFundingInstructionVoidResponse(
//                            FundingInstructionVoidResponse fundingInstructionVoidResponse) {
//                    }
//
//                    public void processGiftCardAuthReversalResponse(GiftCardAuthReversalResponse giftCardAuthReversalResponse) {
//                    }
//
//                    public void processGiftCardCaptureResponse(GiftCardCaptureResponse giftCardCaptureResponse) {
//                    }
//
//                    public void processGiftCardCreditResponse(GiftCardCreditResponse giftCardCreditResponse) {
//                    }
//
//                    public void processFastAccessFundingResponse(FastAccessFundingResponse fastAccessFundingResponse) {
//                        assertEquals("Planets",
//                                fastAccessFundingResponse.getReportGroup());
//                    }
//                })) {
//            txns++;
//        }
//
//        assertEquals(1, txns);
//    }

    private void assertJavaApi(CnpBatchFileRequest request,
            CnpBatchFileResponse response) {
        assertNotNull(response);
        assertNotNull(response.getCnpSessionId());
        assertEquals("0", response.getResponse());
        assertEquals("Valid Format", response.getMessage());
        assertEquals(Versions.XML_VERSION, response.getVersion());

        CnpBatchResponse batchResponse1 = response
                .getNextCnpBatchResponse();
        assertNotNull(batchResponse1);
        assertNotNull(batchResponse1.getCnpBatchId());
        Properties configFromFile = request.getConfig();
        assertEquals(configFromFile.getProperty("merchantId"), batchResponse1.getMerchantId());

        CnpTransactionInterface txnResponse = batchResponse1
                .getNextTransaction();
        SaleResponse saleResponse11 = (SaleResponse) txnResponse;
        assertEquals("000", saleResponse11.getResponse());
        assertEquals("Approved", saleResponse11.getMessage());
        assertNotNull(saleResponse11.getCnpTxnId());
        assertEquals("orderId11", saleResponse11.getOrderId());
        assertEquals("reportGroup11", saleResponse11.getReportGroup());
    }

    private void assertGeneratedFiles(String workingDirRequests,
            String workingDirResponses, String requestFileName,
            CnpBatchFileRequest request, CnpBatchFileResponse response)
            throws Exception {
        File fRequest = request.getFile();
        assertEquals(workingDirRequests + File.separator + requestFileName,
                fRequest.getAbsolutePath());
        assertTrue(fRequest.exists());
        assertTrue(fRequest.length() > 0);

        File fResponse = response.getFile();
        assertEquals(workingDirResponses + File.separator + requestFileName,
                fResponse.getAbsolutePath());
        assertTrue(fResponse.exists());
        assertTrue(fResponse.length() > 0);

        // assert contents of the response file by reading it through the Java
        // API again
        CnpBatchFileResponse responseFromFile = new CnpBatchFileResponse(
                fResponse);
        assertJavaApi(request, responseFromFile);
    }

    private void prepDir(String dirName) {
        File fRequestDir = new File(dirName);
        fRequestDir.mkdirs();
    }

    private void copyFile(File source, File destination) throws IOException {
        FileInputStream sourceIn = null;
        FileOutputStream destOut = null;
        try {
            sourceIn = new FileInputStream(source);
            destOut = new FileOutputStream(destination);

            byte[] buffer = new byte[2048];
            int bytesRead = -1;
            while ((bytesRead = sourceIn.read(buffer)) != -1) {
                destOut.write(buffer, 0, bytesRead);
            }
        } finally {
            if (sourceIn != null) {
                sourceIn.close();
            }
            if (destOut != null) {
                destOut.close();
            }
        }
    }

    class ResponseValidatorProcessor implements CnpResponseProcessor {
        int responseCount = 0;

        public void processAuthorizationResponse(
                AuthorizationResponse authorizationResponse) {
            assertNotNull(authorizationResponse.getCnpTxnId());
            responseCount++;
        }

        public void processCaptureResponse(CaptureResponse captureResponse) {
            assertNotNull(captureResponse.getCnpTxnId());
            responseCount++;
        }

        public void processForceCaptureResponse(
                ForceCaptureResponse forceCaptureResponse) {
            assertNotNull(forceCaptureResponse.getCnpTxnId());
            responseCount++;
        }

        public void processCaptureGivenAuthResponse(
                CaptureGivenAuthResponse captureGivenAuthResponse) {
            assertNotNull(captureGivenAuthResponse.getCnpTxnId());
            responseCount++;
        }

        public void processSaleResponse(SaleResponse saleResponse) {
            assertNotNull(saleResponse.getCnpTxnId());
            responseCount++;
        }

        public void processCreditResponse(CreditResponse creditResponse) {
            assertNotNull(creditResponse.getCnpTxnId());
            responseCount++;
        }

        public void processEcheckSalesResponse(
                EcheckSalesResponse echeckSalesResponse) {
            assertNotNull(echeckSalesResponse.getCnpTxnId());
            responseCount++;
        }

        public void processEcheckCreditResponse(
                EcheckCreditResponse echeckCreditResponse) {
            assertNotNull(echeckCreditResponse.getCnpTxnId());
            responseCount++;
        }

        public void processEcheckVerificationResponse(
                EcheckVerificationResponse echeckVerificationResponse) {
            assertNotNull(echeckVerificationResponse.getCnpTxnId());
            responseCount++;
        }

        public void processEcheckRedepositResponse(
                EcheckRedepositResponse echeckRedepositResponse) {
            assertNotNull(echeckRedepositResponse.getCnpTxnId());
            responseCount++;
        }

        public void processAuthReversalResponse(
                AuthReversalResponse authReversalResponse) {
            assertNotNull(authReversalResponse.getCnpTxnId());
            responseCount++;
        }

        public void processRegisterTokenResponse(
                RegisterTokenResponse registerTokenResponse) {
            assertNotNull(registerTokenResponse.getCnpTxnId());
            responseCount++;
        }

        public void processUpdateSubscriptionResponse(
                UpdateSubscriptionResponse updateSubscriptionResponse) {
            assertNotNull(updateSubscriptionResponse.getCnpTxnId());
            responseCount++;
        }

        public void processCancelSubscriptionResponse(
                CancelSubscriptionResponse cancelSubscriptionResponse) {
            assertNotNull(cancelSubscriptionResponse.getCnpTxnId());
            responseCount++;
        }

        public void processUpdateCardValidationNumOnTokenResponse(
                UpdateCardValidationNumOnTokenResponse updateCardValidationNumOnTokenResponse) {
            assertNotNull(updateCardValidationNumOnTokenResponse.getCnpTxnId());
            responseCount++;
        }

        public void processEcheckPreNoteSaleResponse(
                EcheckPreNoteSaleResponse echeckPreNoteSaleResponse) {
            assertNotNull(echeckPreNoteSaleResponse.getCnpTxnId());
            responseCount++;
        }

        public void processEcheckPreNoteCreditResponse(
                EcheckPreNoteCreditResponse echeckPreNoteCreditResponse) {
            assertNotNull(echeckPreNoteCreditResponse.getCnpTxnId());
            responseCount++;
        }

        public void processAccountUpdate(
                AccountUpdateResponse accountUpdateResponse) {
            assertNotNull(accountUpdateResponse.getCnpTxnId());
            responseCount++;
        }

        public void processCreatePlanResponse(
                CreatePlanResponse createPlanResponse) {
            assertNotNull(createPlanResponse.getCnpTxnId());
            responseCount++;
        }

        public void processUpdatePlanResponse(
                UpdatePlanResponse updatePlanResponse) {
            assertNotNull(updatePlanResponse.getCnpTxnId());
            responseCount++;
        }

        public void processActivateResponse(ActivateResponse activateResponse) {
            assertNotNull(activateResponse.getCnpTxnId());
            responseCount++;
        }

        public void processDeactivateResponse(
                DeactivateResponse deactivateResponse) {
            assertNotNull(deactivateResponse.getCnpTxnId());
            responseCount++;
        }

        public void processLoadResponse(LoadResponse loadResponse) {
            assertNotNull(loadResponse.getCnpTxnId());
            responseCount++;
        }

        public void processUnloadResponse(UnloadResponse unloadResponse) {
            assertNotNull(unloadResponse.getCnpTxnId());
            responseCount++;
        }

        public void processBalanceInquiryResponse(
                BalanceInquiryResponse balanceInquiryResponse) {
            assertNotNull(balanceInquiryResponse.getCnpTxnId());
            responseCount++;
        }

        public void processSubmerchantCreditResponse(
                SubmerchantCreditResponse submerchantCreditResponse) {
            assertNotNull(submerchantCreditResponse.getCnpTxnId());
            responseCount++;
        }

        public void processPayFacCreditResponse(
                PayFacCreditResponse payFacCreditResponse) {
            assertNotNull(payFacCreditResponse.getCnpTxnId());
            responseCount++;
        }

        public void processVendorCreditResponse(
                VendorCreditResponse vendorCreditResponse) {
            assertNotNull(vendorCreditResponse.getCnpTxnId());
            responseCount++;
        }

        public void processReserveCreditResponse(
                ReserveCreditResponse reserveCreditResponse) {
            assertNotNull(reserveCreditResponse.getCnpTxnId());
            responseCount++;
        }

        public void processPhysicalCheckCreditResponse(
                PhysicalCheckCreditResponse physicalCheckCreditResponse) {
            assertNotNull(physicalCheckCreditResponse.getCnpTxnId());
            responseCount++;
        }

        public void processSubmerchantDebitResponse(
                SubmerchantDebitResponse submerchantDebitResponse) {
            assertNotNull(submerchantDebitResponse.getCnpTxnId());
            responseCount++;
        }

        public void processPayFacDebitResponse(
                PayFacDebitResponse payFacDebitResponse) {
            assertNotNull(payFacDebitResponse.getCnpTxnId());
            responseCount++;
        }

        public void processVendorDebitResponse(
                VendorDebitResponse vendorDebitResponse) {
            assertNotNull(vendorDebitResponse.getCnpTxnId());
            responseCount++;
        }

        public void processReserveDebitResponse(
                ReserveDebitResponse reserveDebitResponse) {
            assertNotNull(reserveDebitResponse.getCnpTxnId());
            responseCount++;
        }

        public void processPhysicalCheckDebitResponse(
                PhysicalCheckDebitResponse physicalCheckDebitResponse) {
            assertNotNull(physicalCheckDebitResponse.getCnpTxnId());
            responseCount++;
        }

        public void processFundingInstructionVoidResponse(
                FundingInstructionVoidResponse fundingInstructionVoidResponse) {
            assertNotNull(fundingInstructionVoidResponse.getCnpTxnId());
            responseCount++;
        }

		public void processGiftCardAuthReversalResponse(GiftCardAuthReversalResponse giftCardAuthReversalResponse) {
			assertNotNull(giftCardAuthReversalResponse.getCnpTxnId());
            responseCount++;
		}

		public void processGiftCardCaptureResponse(GiftCardCaptureResponse giftCardCaptureResponse) {
			assertNotNull(giftCardCaptureResponse.getCnpTxnId());
            responseCount++;
		}

		public void processGiftCardCreditResponse(GiftCardCreditResponse giftCardCreditResponse) {
			assertNotNull(giftCardCreditResponse.getCnpTxnId());
            responseCount++;
		}

        public void processFastAccessFundingResponse(FastAccessFundingResponse fastAccessFundingResponse) {
            assertNotNull(fastAccessFundingResponse.getCnpTxnId());
            responseCount++;
        }

        public void processTranslateToLowValueTokenResponse(TranslateToLowValueTokenResponse translateToLowValueTokenResponse){
            assertNotNull(translateToLowValueTokenResponse.getResponse());
            assertNotNull(translateToLowValueTokenResponse.getMessage());
            responseCount++;
        }
    }

    public static void main(String[] args) throws Exception {
        TestBatchFile t = new TestBatchFile();

        t.testSendToCnp_WithFileConfig();
        t.testSendToCnp_WithConfigOverrides();
    }

}
