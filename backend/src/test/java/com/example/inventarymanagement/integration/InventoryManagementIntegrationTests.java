package com.example.inventarymanagement.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InventoryManagementIntegrationTests {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void createSupplier_returnsCreated() throws Exception {
                mockMvc.perform(post("/api/v1/suppliers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(supplierPayload("SUP-001")))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.supplierCode").value("SUP-001"));
        }

        @Test
        @WithMockUser(roles = "WAREHOUSE_STAFF")
        void createSupplier_staffForbidden() throws Exception {
                mockMvc.perform(post("/api/v1/suppliers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(supplierPayload("SUP-NO")))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "PROCUREMENT_OFFICER")
        void createSupplier_procurementAllowed() throws Exception {
                mockMvc.perform(post("/api/v1/suppliers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(supplierPayload("SUP-PRC")))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.supplierCode").value("SUP-PRC"));
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void createProduct_generatesSku() throws Exception {
                long supplierId = createSupplierViaApi("SUP-A");

                mockMvc.perform(post("/api/v1/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(productPayload("Rice", "GROCERY", supplierId)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.sku")
                                                .value(org.hamcrest.Matchers.matchesPattern("SKU-GRO-\\d{4}")));
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void listProducts_filterByCategory_returnsMatchingProducts() throws Exception {
                long supplierId = createSupplierViaApi("SUP-LST1");

                mockMvc.perform(post("/api/v1/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(productPayload("Rice", "GROCERY", supplierId)))
                                .andExpect(status().isCreated());

                mockMvc.perform(post("/api/v1/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(productPayload("TV", "ELECTRONICS", supplierId)))
                                .andExpect(status().isCreated());

                mockMvc.perform(get("/api/v1/products").param("category", "GROCERY"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].category").value("GROCERY"));
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void listProducts_filterLowStock_returnsLowStockItems() throws Exception {
                long productId = createProductViaApi("Sugar", "GROCERY", "SUP-LST2");

                mockMvc.perform(get("/api/v1/products").param("lowStock", "true"))
                                .andExpect(status().isOk());

                mockMvc.perform(get("/api/v1/products/{id}", productId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.product.id").value(productId));
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void createProduct_invalidPayload_returnsBadRequest() throws Exception {
                mockMvc.perform(post("/api/v1/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "name": "",
                                                  "category": "GROCERY",
                                                  "unitPrice": 0,
                                                  "costPrice": 0
                                                }
                                                """))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void updateStock_receipt_increasesQuantity() throws Exception {
                long productId = createProductViaApi("Oil", "GROCERY", "SUP-B");

                mockMvc.perform(patch("/api/v1/products/{id}/stock", productId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "movementType": "RECEIPT",
                                                  "quantity": 15,
                                                  "referenceNumber": "GRN-1"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.quantityOnHand").value(15));
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void updateStock_saleWithoutStock_returnsBadRequest() throws Exception {
                long productId = createProductViaApi("Flour", "GROCERY", "SUP-C");

                mockMvc.perform(patch("/api/v1/products/{id}/stock", productId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "movementType": "SALE",
                                                  "quantity": 5
                                                }
                                                """))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void getProduct_returnsRecentMovements() throws Exception {
                long productId = createProductViaApi("TV", "ELECTRONICS", "SUP-D");

                mockMvc.perform(patch("/api/v1/products/{id}/stock", productId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "movementType": "RECEIPT",
                                                  "quantity": 20
                                                }
                                                """))
                                .andExpect(status().isOk());

                mockMvc.perform(patch("/api/v1/products/{id}/stock", productId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "movementType": "SALE",
                                                  "quantity": 2
                                                }
                                                """))
                                .andExpect(status().isOk());

                mockMvc.perform(get("/api/v1/products/{id}", productId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.recentMovements[0].movementType").value("SALE"));
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void lowStockAlerts_managerAccess_ok() throws Exception {
                long productId = createProductViaApi("Shirt", "CLOTHING", "SUP-E");

                mockMvc.perform(get("/api/v1/stock/low-alerts"))
                                .andExpect(status().isOk());

                mockMvc.perform(get("/api/v1/products/{id}", productId))
                                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "WAREHOUSE_STAFF")
        void lowStockAlerts_staffForbidden() throws Exception {
                mockMvc.perform(get("/api/v1/stock/low-alerts"))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void dashboard_managerAccess_ok() throws Exception {
                mockMvc.perform(get("/api/v1/dashboard"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalProducts").exists());
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void createPurchaseOrder_generatesPoNumberAndDraftStatus() throws Exception {
                long productId = createProductViaApi("Bucket", "HOUSEHOLD", "SUP-PO1");
                long supplierId = getSupplierIdFromProduct(productId);

                mockMvc.perform(post("/api/v1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(orderPayload(supplierId, productId, 10)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.status").value("DRAFT"))
                                .andExpect(jsonPath("$.poNumber").value(org.hamcrest.Matchers.startsWith("PO-")));
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void listOrders_filterByStatus_returnsMatchingOrders() throws Exception {
                long productId = createProductViaApi("Can", "HOUSEHOLD", "SUP-ORDL1");
                long supplierId = getSupplierIdFromProduct(productId);
                long poId = createOrderViaApi(supplierId, productId, 8);

                mockMvc.perform(patch("/api/v1/orders/{id}/status", poId)
                                .param("status", "SUBMITTED"))
                                .andExpect(status().isOk());

                mockMvc.perform(get("/api/v1/orders").param("status", "SUBMITTED"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].status").value("SUBMITTED"));
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void getOrderById_returnsOrderWithItems() throws Exception {
                long productId = createProductViaApi("Pen", "HOUSEHOLD", "SUP-ORDL2");
                long supplierId = getSupplierIdFromProduct(productId);
                long poId = createOrderViaApi(supplierId, productId, 3);

                mockMvc.perform(get("/api/v1/orders/{id}", poId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(poId))
                                .andExpect(jsonPath("$.items[0].productId").value(productId));
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void updatePurchaseOrderStatus_toSubmitted_ok() throws Exception {
                long productId = createProductViaApi("Box", "HOUSEHOLD", "SUP-F");
                long supplierId = getSupplierIdFromProduct(productId);
                long poId = createOrderViaApi(supplierId, productId, 10);

                mockMvc.perform(patch("/api/v1/orders/{id}/status", poId)
                                .param("status", "SUBMITTED"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("SUBMITTED"));
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void receivePurchaseOrder_updatesStockAndStatus() throws Exception {
                long productId = createProductViaApi("Soap", "PERSONAL_CARE", "SUP-G");
                long supplierId = getSupplierIdFromProduct(productId);
                long poId = createOrderViaApi(supplierId, productId, 10);

                mockMvc.perform(patch("/api/v1/orders/{id}/status", poId)
                                .param("status", "SUBMITTED"))
                                .andExpect(status().isOk());

                mockMvc.perform(patch("/api/v1/orders/{id}/receive", poId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("RECEIVED"));

                mockMvc.perform(get("/api/v1/products/{id}", productId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.product.quantityOnHand").value(10));
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void receivePurchaseOrder_draftStatus_returnsBadRequest() throws Exception {
                long productId = createProductViaApi("Detergent", "PERSONAL_CARE", "SUP-H");
                long supplierId = getSupplierIdFromProduct(productId);
                long poId = createOrderViaApi(supplierId, productId, 10);

                mockMvc.perform(patch("/api/v1/orders/{id}/receive", poId))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void supplierCatalog_returnsProducts() throws Exception {
                long supplierId = createSupplierViaApi("SUP-CAT");
                createProductForSupplier("Comb", "PERSONAL_CARE", supplierId);

                mockMvc.perform(get("/api/v1/suppliers/{id}/catalog", supplierId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].name").value("Comb"));
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void getUnknownProduct_returnsNotFound() throws Exception {
                mockMvc.perform(get("/api/v1/products/99999"))
                                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void createPurchaseOrder_withoutItems_returnsBadRequest() throws Exception {
                long supplierId = createSupplierViaApi("SUP-EMPTY");

                mockMvc.perform(post("/api/v1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "supplierId": %d,
                                                  "orderDate": "%s",
                                                  "items": []
                                                }
                                                """.formatted(supplierId, LocalDate.now())))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void unauthenticatedRequest_returnsUnauthorized() throws Exception {
                mockMvc.perform(get("/api/v1/dashboard"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void login_returnsJwtToken() throws Exception {
                registerUserThroughApi("priya.login@example.com", "manager123", "Priya Login", "STORE_MANAGER");

                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "username": "priya.login@example.com",
                                                  "password": "manager123"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                                .andExpect(jsonPath("$.role").value("STORE_MANAGER"));
        }

        @Test
        void registerUser_returnsCreated() throws Exception {
                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "email": "new.warehouse@example.com",
                                                  "password": "newpass123",
                                                  "fullName": "New Warehouse",
                                                  "role": "WAREHOUSE_STAFF"
                                                }
                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.email").value("new.warehouse@example.com"))
                                .andExpect(jsonPath("$.role").value("WAREHOUSE_STAFF"));
        }

        @Test
        void registerThenLogin_withNewUser_returnsJwtToken() throws Exception {
                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "email": "new.manager@example.com",
                                                  "password": "newpass123",
                                                  "fullName": "New Manager",
                                                  "role": "STORE_MANAGER"
                                                }
                                                """))
                                .andExpect(status().isCreated());

                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "username": "new.manager@example.com",
                                                  "password": "newpass123"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                                .andExpect(jsonPath("$.role").value("STORE_MANAGER"));
        }

        @Test
        void dashboard_withManagerJwt_returnsOk() throws Exception {
                registerUserThroughApi("dashboard.manager@example.com", "manager123", "Dashboard Manager",
                                "STORE_MANAGER");

                String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "username": "dashboard.manager@example.com",
                                                  "password": "manager123"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                String token = objectMapper.readTree(loginResponse).get("accessToken").asText();

                mockMvc.perform(get("/api/v1/dashboard")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalProducts").exists());
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void createSupplier_invalidEmail_returnsBadRequest() throws Exception {
                mockMvc.perform(post("/api/v1/suppliers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "name": "Wrong Mail",
                                                  "supplierCode": "SUP-W",
                                                  "contactEmail": "bad-email"
                                                }
                                                """))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void poNumber_incrementsWithinYear() throws Exception {
                long productId = createProductViaApi("Notebook", "HOUSEHOLD", "SUP-I");
                long supplierId = getSupplierIdFromProduct(productId);

                String first = mockMvc.perform(post("/api/v1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(orderPayload(supplierId, productId, 5)))
                                .andExpect(status().isCreated())
                                .andReturn().getResponse().getContentAsString();

                String second = mockMvc.perform(post("/api/v1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(orderPayload(supplierId, productId, 7)))
                                .andExpect(status().isCreated())
                                .andReturn().getResponse().getContentAsString();

                String po1 = objectMapper.readTree(first).get("poNumber").asText();
                String po2 = objectMapper.readTree(second).get("poNumber").asText();
                assertThat(po1).isNotEqualTo(po2);
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void sku_incrementsByCategory() throws Exception {
                long supplierId = createSupplierViaApi("SUP-SKU");

                String first = mockMvc.perform(post("/api/v1/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(productPayload("Soap", "PERSONAL_CARE", supplierId)))
                                .andExpect(status().isCreated())
                                .andReturn().getResponse().getContentAsString();

                String second = mockMvc.perform(post("/api/v1/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(productPayload("Shampoo", "PERSONAL_CARE", supplierId)))
                                .andExpect(status().isCreated())
                                .andReturn().getResponse().getContentAsString();

                String sku1 = objectMapper.readTree(first).get("sku").asText();
                String sku2 = objectMapper.readTree(second).get("sku").asText();

                assertThat(sku1).startsWith("SKU-PRC-");
                assertThat(sku2).startsWith("SKU-PRC-");
                assertThat(sku1).isNotEqualTo(sku2);
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void getAlerts_andResolveAlert_flowWorks() throws Exception {
                createProductViaApi("Alert Product", "GROCERY", "SUP-ALR");

                String alertsBody = mockMvc.perform(get("/api/v1/alerts"))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();

                JsonNode alerts = objectMapper.readTree(alertsBody);
                assertThat(alerts.isArray()).isTrue();
                assertThat(alerts.size()).isGreaterThan(0);

                long alertId = alerts.get(0).get("id").asLong();

                mockMvc.perform(get("/api/v1/alerts/{id}", alertId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(alertId));

                mockMvc.perform(patch("/api/v1/alerts/{id}/resolve", alertId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.resolved").value(true));
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void listStockLevels_andGetByProductId_returnData() throws Exception {
                long productId = createProductViaApi("Stock Level Product", "GROCERY", "SUP-STL");

                mockMvc.perform(get("/api/v1/stock-levels"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].productId").exists());

                mockMvc.perform(get("/api/v1/stock-levels/{productId}", productId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.productId").value(productId));
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void listStockMovements_getById_andProductMovements_returnData() throws Exception {
                long productId = createProductViaApi("Move Product", "GROCERY", "SUP-MOV");

                mockMvc.perform(patch("/api/v1/products/{id}/stock", productId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "movementType": "RECEIPT",
                                                  "quantity": 9,
                                                  "referenceNumber": "RCV-9001",
                                                  "notes": "initial receive"
                                                }
                                                """))
                                .andExpect(status().isOk());

                String allMovements = mockMvc.perform(get("/api/v1/stock-movements"))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();

                long movementId = objectMapper.readTree(allMovements).get(0).get("id").asLong();

                mockMvc.perform(get("/api/v1/stock-movements/{id}", movementId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(movementId));

                mockMvc.perform(get("/api/v1/products/{id}/movements", productId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].movementType").value("RECEIPT"));
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void updateStatus_invalidTransition_returnsBadRequest() throws Exception {
                long productId = createProductViaApi("PO Transitions", "HOUSEHOLD", "SUP-TRN");
                long supplierId = getSupplierIdFromProduct(productId);
                long poId = createOrderViaApi(supplierId, productId, 4);

                mockMvc.perform(patch("/api/v1/orders/{id}/status", poId)
                                .param("status", "ACKNOWLEDGED"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void alertGeneration_preventsDuplicateUnresolvedOutOfStockAlerts() throws Exception {
                long productId = createProductViaApi("Duplicate Alert Product", "GROCERY", "SUP-DUP-ALR");

                mockMvc.perform(patch("/api/v1/products/{id}/stock", productId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "movementType": "RECEIPT",
                                                  "quantity": 1,
                                                  "referenceNumber": "R1"
                                                }
                                                """))
                                .andExpect(status().isOk());

                mockMvc.perform(patch("/api/v1/products/{id}/stock", productId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "movementType": "SALE",
                                                  "quantity": 1,
                                                  "referenceNumber": "S1"
                                                }
                                                """))
                                .andExpect(status().isOk());

                String alertsBody = mockMvc.perform(get("/api/v1/alerts"))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();

                JsonNode alerts = objectMapper.readTree(alertsBody);
                long outOfStockCount = 0;
                for (JsonNode alert : alerts) {
                        boolean sameProduct = alert.get("productId").asLong() == productId;
                        boolean outOfStock = "OUT_OF_STOCK".equals(alert.get("alertType").asText());
                        boolean unresolved = !alert.get("resolved").asBoolean();
                        if (sameProduct && outOfStock && unresolved) {
                                outOfStockCount++;
                        }
                }

                assertThat(outOfStockCount).isEqualTo(1);
        }

        private long createSupplierViaApi(String code) throws Exception {
                String response = mockMvc.perform(post("/api/v1/suppliers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(supplierPayload(code)))
                                .andExpect(status().isCreated())
                                .andReturn().getResponse().getContentAsString();
                return objectMapper.readTree(response).get("id").asLong();
        }

        private long createProductViaApi(String name, String category, String supplierCode) throws Exception {
                long supplierId = createSupplierViaApi(supplierCode);
                return createProductForSupplier(name, category, supplierId);
        }

        private long createProductForSupplier(String name, String category, long supplierId) throws Exception {
                String response = mockMvc.perform(post("/api/v1/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(productPayload(name, category, supplierId)))
                                .andExpect(status().isCreated())
                                .andReturn().getResponse().getContentAsString();
                return objectMapper.readTree(response).get("id").asLong();
        }

        private long getSupplierIdFromProduct(long productId) throws Exception {
                String response = mockMvc.perform(get("/api/v1/products/{id}", productId))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();
                JsonNode productNode = objectMapper.readTree(response).get("product");
                return productNode.get("supplierId").asLong();
        }

        private long createOrderViaApi(long supplierId, long productId, int quantity) throws Exception {
                String response = mockMvc.perform(post("/api/v1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(orderPayload(supplierId, productId, quantity)))
                                .andExpect(status().isCreated())
                                .andReturn().getResponse().getContentAsString();
                return objectMapper.readTree(response).get("id").asLong();
        }

        private String supplierPayload(String supplierCode) {
                return """
                                {
                                  "name": "Supplier %s",
                                  "supplierCode": "%s",
                                  "contactEmail": "%s@example.com",
                                  "paymentTermsDays": 30,
                                  "leadTimeDays": 7
                                }
                                """.formatted(supplierCode, supplierCode, supplierCode.toLowerCase());
        }

        private String productPayload(String name, String category, long supplierId) {
                return """
                                {
                                  "name": "%s",
                                  "category": "%s",
                                  "unitPrice": 100.00,
                                  "costPrice": 70.00,
                                  "unitOfMeasure": "pieces",
                                  "reorderPoint": 10,
                                  "reorderQuantity": 40,
                                  "supplierId": %d
                                }
                                """.formatted(name, category, supplierId);
        }

        private String orderPayload(long supplierId, long productId, int quantity) {
                return """
                                {
                                  "supplierId": %d,
                                  "orderDate": "%s",
                                  "expectedDelivery": "%s",
                                  "items": [
                                    {
                                      "productId": %d,
                                      "quantityOrdered": %d,
                                      "unitCost": 70.00
                                    }
                                  ]
                                }
                                """.formatted(supplierId, LocalDate.now(), LocalDate.now().plusDays(2), productId,
                                quantity);
        }

        private void registerUserThroughApi(String email, String password, String fullName, String role)
                        throws Exception {
                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                        "email": "%s",
                                                        "password": "%s",
                                                        "fullName": "%s",
                                                        "role": "%s"
                                                }
                                                """.formatted(email, password, fullName, role)))
                                .andExpect(status().isCreated());
        }
}
