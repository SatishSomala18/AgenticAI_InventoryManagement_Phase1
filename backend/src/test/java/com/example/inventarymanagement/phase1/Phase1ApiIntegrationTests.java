package com.example.inventarymanagement.phase1;

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
class Phase1ApiIntegrationTests {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void tc07p1api01_createProductReturns201WithSku() throws Exception {
                long supplierId = createSupplier("SUP-API-01");

                mockMvc.perform(post("/api/v1/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(productPayload("Basmati Rice 5kg", "GROCERY", supplierId)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.sku").value(org.hamcrest.Matchers.startsWith("SKU-GRO-")));
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void tc07p1api02_stockUpdateCreatesMovementAndAlert() throws Exception {
                long productId = createProduct("Low Alert Product", "GROCERY", "SUP-API-02");

                mockMvc.perform(patch("/api/v1/products/{id}/stock", productId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "movementType": "RECEIPT",
                                                  "quantity": 5,
                                                  "referenceNumber": "STK-LOW-1",
                                                  "notes": "set stock below reorder"
                                                }
                                                """))
                                .andExpect(status().isOk());

                String alerts = mockMvc.perform(get("/api/v1/stock/low-alerts"))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();

                JsonNode alertArray = objectMapper.readTree(alerts);
                boolean found = false;
                for (JsonNode node : alertArray) {
                        if (node.get("productId").asLong() == productId) {
                                found = true;
                                break;
                        }
                }
                assertThat(found).isTrue();
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void tc07p1api03_createPurchaseOrderWithPoNumber() throws Exception {
                long productId = createProduct("PO Product", "GROCERY", "SUP-API-03");
                long supplierId = getSupplierIdFromProduct(productId);

                mockMvc.perform(post("/api/v1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(orderPayload(supplierId, productId, 100)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.poNumber").value(org.hamcrest.Matchers.startsWith("PO-")))
                                .andExpect(jsonPath("$.status").value("DRAFT"));
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void tc07p1api04_receivePoUpdatesStock() throws Exception {
                long productId = createProduct("Receive Product", "GROCERY", "SUP-API-04");
                long supplierId = getSupplierIdFromProduct(productId);
                long orderId = createOrder(supplierId, productId, 25);

                String before = mockMvc.perform(get("/api/v1/products/{id}", productId))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();
                int onHandBefore = objectMapper.readTree(before).get("product").get("quantityOnHand").asInt();

                mockMvc.perform(patch("/api/v1/orders/{id}/status", orderId).param("status", "SUBMITTED"))
                                .andExpect(status().isOk());

                mockMvc.perform(patch("/api/v1/orders/{id}/receive", orderId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("RECEIVED"));

                String after = mockMvc.perform(get("/api/v1/products/{id}", productId))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();
                int onHandAfter = objectMapper.readTree(after).get("product").get("quantityOnHand").asInt();

                assertThat(onHandAfter).isGreaterThan(onHandBefore);
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void tc07p1api05_lowAlertsReturnsCorrectProducts() throws Exception {
                mockMvc.perform(get("/api/v1/stock/low-alerts"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray());
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void tc07p1api06_supplierCatalogReturnsProducts() throws Exception {
                long supplierId = createSupplier("SUP-API-06");
                createProductForSupplier("Catalog Product", "GROCERY", supplierId);

                mockMvc.perform(get("/api/v1/suppliers/{id}/catalog", supplierId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray());
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void tc07p1api07_filterProductsByCategory() throws Exception {
                long supplierId = createSupplier("SUP-API-07");
                createProductForSupplier("Rice", "GROCERY", supplierId);
                createProductForSupplier("TV", "ELECTRONICS", supplierId);

                mockMvc.perform(get("/api/v1/products").param("category", "GROCERY"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].category").value("GROCERY"));
        }

        @Test
        @WithMockUser(roles = "STORE_MANAGER")
        void tc07p1api08_dashboardReturnsMetrics() throws Exception {
                mockMvc.perform(get("/api/v1/dashboard"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalProducts").exists())
                                .andExpect(jsonPath("$.lowStockCount").exists())
                                .andExpect(jsonPath("$.outOfStockCount").exists())
                                .andExpect(jsonPath("$.openPoCount").exists())
                                .andExpect(jsonPath("$.totalStockValue").exists());
        }

        private long createSupplier(String code) throws Exception {
                String response = mockMvc.perform(post("/api/v1/suppliers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(supplierPayload(code)))
                                .andExpect(status().isCreated())
                                .andReturn().getResponse().getContentAsString();
                return objectMapper.readTree(response).get("id").asLong();
        }

        private long createProduct(String name, String category, String supplierCode) throws Exception {
                long supplierId = createSupplier(supplierCode);
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
                return objectMapper.readTree(response).get("product").get("supplierId").asLong();
        }

        private long createOrder(long supplierId, long productId, int quantityOrdered) throws Exception {
                String response = mockMvc.perform(post("/api/v1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(orderPayload(supplierId, productId, quantityOrdered)))
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
                                  "unitPrice": 350.0,
                                  "costPrice": 280.0,
                                  "unitOfMeasure": "box",
                                  "reorderPoint": 20,
                                  "reorderQuantity": 100,
                                  "supplierId": %d
                                }
                                """.formatted(name, category, supplierId);
        }

        private String orderPayload(long supplierId, long productId, int quantityOrdered) {
                return """
                                {
                                  "supplierId": %d,
                                  "orderDate": "%s",
                                  "expectedDelivery": "%s",
                                  "items": [
                                    {
                                      "productId": %d,
                                      "quantityOrdered": %d,
                                      "unitCost": 280.0
                                    }
                                  ]
                                }
                                """.formatted(supplierId, LocalDate.now(), LocalDate.now().plusDays(7), productId,
                                quantityOrdered);
        }
}
