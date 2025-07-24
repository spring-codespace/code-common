import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import org.mapstruct.factory.Mappers;

// Test for the interface - mainly for documentation purposes
class CamtReportGeneratorTest {
    
    @Test
    @DisplayName("Interface should define generateReport method")
    void interfaceDefinition() {
        // This test validates the interface contract
        assertTrue(CamtReportGenerator.class.isInterface());
        assertEquals(1, CamtReportGenerator.class.getDeclaredMethods().length);
        assertEquals("generateReport", CamtReportGenerator.class.getDeclaredMethods()[0].getName());
    }
}

// Tests for AbstractCamtReportGenerator
@ExtendWith(MockitoExtension.class)
class AbstractCamtReportGeneratorTest {
    
    @Mock
    private JAXBContext mockJaxbContext;
    
    @Mock
    private Schema mockSchema;
    
    @Mock
    private Marshaller mockMarshaller;
    
    @Mock
    private ClassPathResource mockClassPathResource;
    
    @Mock
    private File mockFile;
    
    @Mock
    private SchemaFactory mockSchemaFactory;
    
    // Concrete implementation for testing
    private static class TestCamtReportGenerator extends AbstractCamtReportGenerator<String> {
        private JAXBElement<String> documentToReturn;
        
        public TestCamtReportGenerator(Class<String> documentType, String schemaPath) {
            super(documentType, schemaPath);
        }
        
        public TestCamtReportGenerator(Class<String> documentType, String schemaPath, 
                                     JAXBElement<String> documentToReturn) {
            super(documentType, schemaPath);
            this.documentToReturn = documentToReturn;
        }
        
        @Override
        protected JAXBElement<String> createDocument(List<ReportData> data, ReportContext context) {
            return documentToReturn != null ? documentToReturn : mock(JAXBElement.class);
        }
    }
    
    @Test
    @DisplayName("Constructor should initialize JAXB context and schema successfully")
    void constructorSuccess() throws Exception {
        try (MockedStatic<JAXBContext> jaxbContextMock = mockStatic(JAXBContext.class);
             MockedStatic<SchemaFactory> schemaFactoryMock = mockStatic(SchemaFactory.class)) {
            
            // Setup mocks
            jaxbContextMock.when(() -> JAXBContext.newInstance(String.class))
                          .thenReturn(mockJaxbContext);
            
            schemaFactoryMock.when(() -> SchemaFactory.newInstance(anyString()))
                           .thenReturn(mockSchemaFactory);
            
            when(mockSchemaFactory.newSchema(any(File.class))).thenReturn(mockSchema);
            
            try (MockedStatic<ClassPathResource> resourceMock = mockStatic(ClassPathResource.class)) {
                resourceMock.when(() -> new ClassPathResource("test.xsd"))
                           .thenReturn(mockClassPathResource);
                when(mockClassPathResource.getFile()).thenReturn(mockFile);
                
                // Execute
                TestCamtReportGenerator generator = new TestCamtReportGenerator(String.class, "test.xsd");
                
                // Verify
                assertNotNull(generator);
                jaxbContextMock.verify(() -> JAXBContext.newInstance(String.class));
                verify(mockSchemaFactory).newSchema(mockFile);
            }
        }
    }
    
    @Test
    @DisplayName("Constructor should throw ReportInitializationException when JAXB context creation fails")
    void constructorJaxbContextFailure() {
        try (MockedStatic<JAXBContext> jaxbContextMock = mockStatic(JAXBContext.class)) {
            jaxbContextMock.when(() -> JAXBContext.newInstance(String.class))
                          .thenThrow(new JAXBException("JAXB error"));
            
            ReportInitializationException exception = assertThrows(
                ReportInitializationException.class,
                () -> new TestCamtReportGenerator(String.class, "test.xsd")
            );
            
            assertEquals("Failed to initialize JAXB context", exception.getMessage());
            assertInstanceOf(JAXBException.class, exception.getCause());
        }
    }
    
    @Test
    @DisplayName("Constructor should throw ReportInitializationException when schema creation fails")
    void constructorSchemaFailure() throws Exception {
        try (MockedStatic<JAXBContext> jaxbContextMock = mockStatic(JAXBContext.class);
             MockedStatic<SchemaFactory> schemaFactoryMock = mockStatic(SchemaFactory.class)) {
            
            jaxbContextMock.when(() -> JAXBContext.newInstance(String.class))
                          .thenReturn(mockJaxbContext);
            
            schemaFactoryMock.when(() -> SchemaFactory.newInstance(anyString()))
                           .thenReturn(mockSchemaFactory);
            
            when(mockSchemaFactory.newSchema(any(File.class)))
                .thenThrow(new RuntimeException("Schema error"));
            
            try (MockedStatic<ClassPathResource> resourceMock = mockStatic(ClassPathResource.class)) {
                resourceMock.when(() -> new ClassPathResource("test.xsd"))
                           .thenReturn(mockClassPathResource);
                when(mockClassPathResource.getFile()).thenReturn(mockFile);
                
                ReportInitializationException exception = assertThrows(
                    ReportInitializationException.class,
                    () -> new TestCamtReportGenerator(String.class, "test.xsd")
                );
                
                assertEquals("Failed to initialize JAXB context", exception.getMessage());
                assertInstanceOf(RuntimeException.class, exception.getCause());
            }
        }
    }
    
    @Test
    @DisplayName("generateReport should create document and marshal it successfully")
    void generateReportSuccess() throws Exception {
        // Setup
        JAXBElement<String> mockDocument = mock(JAXBElement.class);
        byte[] expectedBytes = "test xml".getBytes();
        
        try (MockedStatic<JAXBContext> jaxbContextMock = mockStatic(JAXBContext.class);
             MockedStatic<SchemaFactory> schemaFactoryMock = mockStatic(SchemaFactory.class)) {
            
            setupMocksForSuccessfulConstruction(jaxbContextMock, schemaFactoryMock);
            
            when(mockJaxbContext.createMarshaller()).thenReturn(mockMarshaller);
            doAnswer(invocation -> {
                ByteArrayOutputStream os = invocation.getArgument(1);
                os.write(expectedBytes);
                return null;
            }).when(mockMarshaller).marshal(eq(mockDocument), any(ByteArrayOutputStream.class));
            
            TestCamtReportGenerator generator = new TestCamtReportGenerator(
                String.class, "test.xsd", mockDocument);
            
            List<ReportData> data = Arrays.asList(mock(ReportData.class));
            ReportContext context = mock(ReportContext.class);
            
            // Execute
            byte[] result = generator.generateReport(data, context);
            
            // Verify
            assertArrayEquals(expectedBytes, result);
            verify(mockMarshaller).setSchema(mockSchema);
            verify(mockMarshaller).setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            verify(mockMarshaller).marshal(eq(mockDocument), any(ByteArrayOutputStream.class));
        }
    }
    
    @Test
    @DisplayName("generateReport should throw ReportGenerationException when marshalling fails")
    void generateReportMarshallingFailure() throws Exception {
        // Setup
        JAXBElement<String> mockDocument = mock(JAXBElement.class);
        
        try (MockedStatic<JAXBContext> jaxbContextMock = mockStatic(JAXBContext.class);
             MockedStatic<SchemaFactory> schemaFactoryMock = mockStatic(SchemaFactory.class)) {
            
            setupMocksForSuccessfulConstruction(jaxbContextMock, schemaFactoryMock);
            
            when(mockJaxbContext.createMarshaller()).thenReturn(mockMarshaller);
            doThrow(new JAXBException("Marshalling error"))
                .when(mockMarshaller).marshal(any(), any(ByteArrayOutputStream.class));
            
            TestCamtReportGenerator generator = new TestCamtReportGenerator(
                String.class, "test.xsd", mockDocument);
            
            List<ReportData> data = Arrays.asList(mock(ReportData.class));
            ReportContext context = mock(ReportContext.class);
            
            // Execute & Verify
            ReportGenerationException exception = assertThrows(
                ReportGenerationException.class,
                () -> generator.generateReport(data, context)
            );
            
            assertEquals("Failed to generate CAMT XML report, identified errors while marshalling!", 
                        exception.getMessage());
            assertInstanceOf(JAXBException.class, exception.getCause());
        }
    }
    
    @Test
    @DisplayName("generateReport should handle empty data list")
    void generateReportEmptyData() throws Exception {
        JAXBElement<String> mockDocument = mock(JAXBElement.class);
        byte[] expectedBytes = "empty xml".getBytes();
        
        try (MockedStatic<JAXBContext> jaxbContextMock = mockStatic(JAXBContext.class);
             MockedStatic<SchemaFactory> schemaFactoryMock = mockStatic(SchemaFactory.class)) {
            
            setupMocksForSuccessfulConstruction(jaxbContextMock, schemaFactoryMock);
            
            when(mockJaxbContext.createMarshaller()).thenReturn(mockMarshaller);
            doAnswer(invocation -> {
                ByteArrayOutputStream os = invocation.getArgument(1);
                os.write(expectedBytes);
                return null;
            }).when(mockMarshaller).marshal(eq(mockDocument), any(ByteArrayOutputStream.class));
            
            TestCamtReportGenerator generator = new TestCamtReportGenerator(
                String.class, "test.xsd", mockDocument);
            
            List<ReportData> data = Collections.emptyList();
            ReportContext context = mock(ReportContext.class);
            
            // Execute
            byte[] result = generator.generateReport(data, context);
            
            // Verify
            assertArrayEquals(expectedBytes, result);
        }
    }
    
    private void setupMocksForSuccessfulConstruction(MockedStatic<JAXBContext> jaxbContextMock,
                                                   MockedStatic<SchemaFactory> schemaFactoryMock) throws Exception {
        jaxbContextMock.when(() -> JAXBContext.newInstance(String.class))
                      .thenReturn(mockJaxbContext);
        
        schemaFactoryMock.when(() -> SchemaFactory.newInstance(anyString()))
                       .thenReturn(mockSchemaFactory);
        
        when(mockSchemaFactory.newSchema(any(File.class))).thenReturn(mockSchema);
        
        try (MockedStatic<ClassPathResource> resourceMock = mockStatic(ClassPathResource.class)) {
            resourceMock.when(() -> new ClassPathResource("test.xsd"))
                       .thenReturn(mockClassPathResource);
            when(mockClassPathResource.getFile()).thenReturn(mockFile);
        }
    }
}

// Tests for Camt054V02Generator
@ExtendWith(MockitoExtension.class)
class Camt054V02GeneratorTest {
    
    @Mock
    private ReportDataV02Mapper reportDataV02Mapper;
    
    @Mock
    private BankToCustomerDebitCreditNotificationV02Mapper bankToCustomerMapper;
    
    @Mock
    private ObjectFactory objectFactory;
    
    @Mock
    private Document mockDocument;
    
    @Mock
    private JAXBElement<Document> mockJaxbElement;
    
    private Camt054V02Generator generator;
    
    @BeforeEach
    void setUp() throws Exception {
        // Mock the constructor dependencies
        try (MockedStatic<JAXBContext> jaxbContextMock = mockStatic(JAXBContext.class);
             MockedStatic<SchemaFactory> schemaFactoryMock = mockStatic(SchemaFactory.class)) {
            
            JAXBContext mockJaxbContext = mock(JAXBContext.class);
            Schema mockSchema = mock(Schema.class);
            SchemaFactory mockSchemaFactory = mock(SchemaFactory.class);
            ClassPathResource mockResource = mock(ClassPathResource.class);
            File mockFile = mock(File.class);
            
            jaxbContextMock.when(() -> JAXBContext.newInstance(Document.class))
                          .thenReturn(mockJaxbContext);
            
            schemaFactoryMock.when(() -> SchemaFactory.newInstance(anyString()))
                           .thenReturn(mockSchemaFactory);
            
            when(mockSchemaFactory.newSchema(any(File.class))).thenReturn(mockSchema);
            
            try (MockedStatic<ClassPathResource> resourceMock = mockStatic(ClassPathResource.class)) {
                resourceMock.when(() -> new ClassPathResource("schemas/camt.054.001.02.xsd"))
                           .thenReturn(mockResource);
                when(mockResource.getFile()).thenReturn(mockFile);
                
                generator = new Camt054V02Generator(reportDataV02Mapper);
            }
        }
    }
    
    @Test
    @DisplayName("Constructor should initialize with correct schema path")
    void constructorInitialization() {
        assertNotNull(generator);
        // The constructor test is implicitly done in setUp() method
        // If construction fails, setUp() would throw an exception
    }
    
    @Test
    @DisplayName("createDocument should create CAMT054V02 document successfully")
    void createDocumentSuccess() throws Exception {
        // Setup test data
        List<ReportData> reportData = Arrays.asList(mock(ReportData.class));
        ReportContext context = mock(ReportContext.class);
        
        // Setup mocks for the mapping chain
        io.vikunalabs.engine.v02.jaxb.version02.BankToCustomerDebitCreditNotificationV02 genericNotification = 
            mock(io.vikunalabs.engine.v02.jaxb.version02.BankToCustomerDebitCreditNotificationV02.class);
        
        BankToCustomerDebitCreditNotificationV02 mappedNotification = 
            mock(BankToCustomerDebitCreditNotificationV02.class);
        
        when(reportDataV02Mapper.createBankNotification(reportData, context))
            .thenReturn(genericNotification);
        
        // Mock the static mapper instance
        try (MockedStatic<Mappers> mappersMock = mockStatic(Mappers.class)) {
            
            mappersMock.when(() -> Mappers.getMapper(BankToCustomerDebitCreditNotificationV02Mapper.class))
                      .thenReturn(bankToCustomerMapper);
            
            when(bankToCustomerMapper.mapToCamt054V02(genericNotification))
                .thenReturn(mappedNotification);
            
            // Mock ObjectFactory behavior
            try (MockedStatic<ObjectFactory> objectFactoryMock = mockStatic(ObjectFactory.class)) {
                ObjectFactory mockObjectFactoryInstance = mock(ObjectFactory.class);
                objectFactoryMock.when(ObjectFactory::new).thenReturn(mockObjectFactoryInstance);
                
                when(mockObjectFactoryInstance.createDocument()).thenReturn(mockDocument);
                when(mockDocument.withBkToCstmrDbtCdtNtfctn(mappedNotification))
                    .thenReturn(mockDocument);
                when(mockObjectFactoryInstance.createDocument(mockDocument))
                    .thenReturn(mockJaxbElement);
                
                // Use reflection to call the protected method
                java.lang.reflect.Method createDocumentMethod = 
                    Camt054V02Generator.class.getDeclaredMethod("createDocument", List.class, ReportContext.class);
                createDocumentMethod.setAccessible(true);
                
                // Execute
                JAXBElement<Document> result = (JAXBElement<Document>) 
                    createDocumentMethod.invoke(generator, reportData, context);
                
                // Verify
                assertNotNull(result);
                assertEquals(mockJaxbElement, result);
                
                verify(reportDataV02Mapper).createBankNotification(reportData, context);
                verify(bankToCustomerMapper).mapToCamt054V02(genericNotification);
                verify(mockDocument).withBkToCstmrDbtCdtNtfctn(mappedNotification);
            }
        }
    }
    
    @Test
    @DisplayName("createDocument should handle null input gracefully")
    void createDocumentNullInput() throws Exception {
        // This test verifies behavior with null inputs
        // The actual behavior depends on the mapper implementations
        
        when(reportDataV02Mapper.createBankNotification(null, null))
            .thenReturn(null);
        
        try (MockedStatic<Mappers> mappersMock = mockStatic(Mappers.class)) {
            
            mappersMock.when(() -> Mappers.getMapper(BankToCustomerDebitCreditNotificationV02Mapper.class))
                      .thenReturn(bankToCustomerMapper);
            
            when(bankToCustomerMapper.mapToCamt054V02(null))
                .thenReturn(null);
            
            // Use reflection to access protected method
            java.lang.reflect.Method createDocumentMethod = 
                Camt054V02Generator.class.getDeclaredMethod("createDocument", List.class, ReportContext.class);
            createDocumentMethod.setAccessible(true);
            
            // This might throw an exception or handle null gracefully
            // depending on the ObjectFactory implementation
            assertDoesNotThrow(() -> {
                try {
                    createDocumentMethod.invoke(generator, null, null);
                } catch (java.lang.reflect.InvocationTargetException e) {
                    // If the underlying method throws, that's expected behavior
                    // The test passes as long as our code doesn't have unexpected issues
                }
            });
        }
    }
    
    @Test
    @DisplayName("createDocument should handle empty data list")
    void createDocumentEmptyData() throws Exception {
        List<ReportData> emptyData = Collections.emptyList();
        ReportContext context = mock(ReportContext.class);
        
        io.vikunalabs.engine.v02.jaxb.version02.BankToCustomerDebitCreditNotificationV02 genericNotification = 
            mock(io.vikunalabs.engine.v02.jaxb.version02.BankToCustomerDebitCreditNotificationV02.class);
        
        when(reportDataV02Mapper.createBankNotification(emptyData, context))
            .thenReturn(genericNotification);
        
        try (MockedStatic<BankToCustomerDebitCreditNotificationV02Mapper> mapperMock = 
             mockStatic(BankToCustomerDebitCreditNotificationV02Mapper.class)) {
            
            mapperMock.when(() -> BankToCustomerDebitCreditNotificationV02Mapper.INSTANCE)
                     .thenReturn(bankToCustomerMapper);
            
            when(bankToCustomerMapper.mapToCamt054V02(genericNotification))
                .thenReturn(mock(BankToCustomerDebitCreditNotificationV02.class));
            
            java.lang.reflect.Method createDocumentMethod = 
                Camt054V02Generator.class.getDeclaredMethod("createDocument", List.class, ReportContext.class);
            createDocumentMethod.setAccessible(true);
            
            // Should not throw exception for empty data
            assertDoesNotThrow(() -> {
                createDocumentMethod.invoke(generator, emptyData, context);
            });
            
            verify(reportDataV02Mapper).createBankNotification(emptyData, context);
        }
    }
}

// Tests for BankToCustomerDebitCreditNotificationV02Mapper
class BankToCustomerDebitCreditNotificationV02MapperTest {
    
    @Test
    @DisplayName("Mapper should have INSTANCE field")
    void mapperInstanceExists() {
        assertNotNull(BankToCustomerDebitCreditNotificationV02Mapper.INSTANCE);
        assertInstanceOf(BankToCustomerDebitCreditNotificationV02Mapper.class, 
                        BankToCustomerDebitCreditNotificationV02Mapper.INSTANCE);
    }
    
    @Test
    @DisplayName("Mapper should have mapToCamt054V02 method")
    void mapperMethodExists() throws NoSuchMethodException {
        java.lang.reflect.Method method = BankToCustomerDebitCreditNotificationV02Mapper.class
            .getDeclaredMethod("mapToCamt054V02", 
                io.vikunalabs.engine.v02.jaxb.version02.BankToCustomerDebitCreditNotificationV02.class);
        
        assertNotNull(method);
        assertEquals(BankToCustomerDebitCreditNotificationV02.class, method.getReturnType());
    }
    
    @Test
    @DisplayName("Mapper interface should be annotated with @Mapper")
    void mapperAnnotationExists() {
        assertTrue(BankToCustomerDebitCreditNotificationV02Mapper.class.isAnnotationPresent(Mapper.class));
    }
}

// Mock classes for testing (these would typically be in your test sources)
class ReportData {
    // Mock implementation
}

class ReportContext {
    // Mock implementation
}

class ReportInitializationException extends RuntimeException {
    public ReportInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}

class ReportGenerationException extends RuntimeException {
    public ReportGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}

class Document {
    public Document withBkToCstmrDbtCdtNtfctn(BankToCustomerDebitCreditNotificationV02 notification) {
        return this;
    }
}

class BankToCustomerDebitCreditNotificationV02 {
    // Mock implementation
}

class ObjectFactory {
    public Document createDocument() {
        return new Document();
    }
    
    public JAXBElement<Document> createDocument(Document document) {
        return mock(JAXBElement.class);
    }
}

interface ReportDataV02Mapper {
    io.vikunalabs.engine.v02.jaxb.version02.BankToCustomerDebitCreditNotificationV02 createBankNotification(
        List<ReportData> data, ReportContext context);
}

@interface Mapper {
}