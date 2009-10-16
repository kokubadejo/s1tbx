package org.esa.beam.gpf.common.reproject;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.gpf.OperatorException;
import static org.junit.Assert.*;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ReprojectionOpTest extends AbstractReprojectionOpTest {

    @Test
    public void testGeoLatLon() throws IOException {
        parameterMap.put("crsCode", WGS84_CODE);
        final Product targetPoduct = createReprojectedProduct();

        assertNotNull(targetPoduct);
        // because source is rectangular the size of source is preserved
        assertEquals(50, targetPoduct.getSceneRasterWidth());
        assertEquals(50, targetPoduct.getSceneRasterHeight());
        assertNotNull(targetPoduct.getGeoCoding());

        testPixelValue(targetPoduct, 23.5f, 13.5f, 299, 1.0e-6);
    }
    
    @Test
    public void testUTMWithWktText() throws IOException {
        parameterMap.put("wkt", UTM33N_WKT);
        final Product targetPoduct = createReprojectedProduct();
        
        assertNotNull(targetPoduct);
        testPixelValue(targetPoduct, 23.5f, 13.5f, 299, 1.0e-6);
    }
    
    @Test
    public void testWithWktFile() throws IOException {
        parameterMap.put("wktFile", wktFile);
        final Product targetPoduct = createReprojectedProduct();

        assertNotNull(targetPoduct);
        testPixelValue(targetPoduct, 23.5f, 13.5f, 299, 1.0e-6);
    }
    
    @Test
    public void testWithCollocationProduct() {
        Map<String, Product> productMap = new HashMap<String, Product>(5);
        productMap.put("source", sourceProduct);
        parameterMap.put("crsCode", "AUTO:42002");
        final Product collocationProduct = createReprojectedProduct(productMap);

        productMap = new HashMap<String, Product>(5);
        productMap.put("source", sourceProduct);
        productMap.put("collocate", collocationProduct);
        parameterMap.remove("crsCode");
        final Product targetProduct = createReprojectedProduct(productMap);
        assertNotNull(targetProduct);
        assertTrue(targetProduct.isCompatibleProduct(collocationProduct, 1.0e-6f));
    }
    
    @Test
    public void testUTM() throws IOException {
        parameterMap.put("crsCode", UTM33N_CODE);
        final Product targetPoduct = createReprojectedProduct();
        
        assertNotNull(targetPoduct);
        testPixelValue(targetPoduct, 23.5f, 13.5f, 299, 1.0e-6);
    }
    
    @Test
    public void testUTM_Bilinear() throws IOException {
        parameterMap.put("crsCode", UTM33N_CODE);
        parameterMap.put("resampling", "Bilinear");
        final Product targetPoduct = createReprojectedProduct();
        
        assertNotNull(targetPoduct);
        assertNotNull(targetPoduct.getGeoCoding());
        // 299, 312
        // 322, 336
        // interpolated = 317.25 for pixel (24, 14)
        testPixelValue(targetPoduct, 24.0f, 14.0f, 317.25, 1.0e-2);
    }

    @Test
    public void testSpecifyingTargetDimension() throws IOException {
        final int width = 200;
        final int height = 300;
        parameterMap.put("crsCode", WGS84_CODE);
        parameterMap.put("width", width);
        parameterMap.put("height", height);
        final Product targetPoduct = createReprojectedProduct();

        assertNotNull(targetPoduct);
        assertEquals(width, targetPoduct.getSceneRasterWidth());
        assertEquals(height, targetPoduct.getSceneRasterHeight());

        testPixelValue(targetPoduct, 23.5f, 13.5f, 299, 1.0e-6);
    }

    @Test
    public void testSpecifyingPixelSize() throws IOException {
        final double sizeX = 5; // degree
        final double sizeY = 10;// degree
        parameterMap.put("crsCode", WGS84_CODE);
        parameterMap.put("pixelSizeX", sizeX);
        parameterMap.put("pixelSizeY", sizeY);
        final Product targetPoduct = createReprojectedProduct();
        
        assertNotNull(targetPoduct);
        // 20° Width / 5° PixelSizeX = 4 SceneWidth
        assertEquals(4, targetPoduct.getSceneRasterWidth());
        // 20° Height / 10° PixelSizeX = 2 SceneHeight
        assertEquals(2, targetPoduct.getSceneRasterHeight());
    }
    
    @Test
    public void testSpecifyingReferencing() throws IOException {
        parameterMap.put("crsCode", WGS84_CODE);
        parameterMap.put("referencePixelX", 0.5);
        parameterMap.put("referencePixelY", 0.5);
        parameterMap.put("easting", 9.0);   // just move it 3° degrees eastward
        parameterMap.put("northing", 52.0); // just move it 2° degrees up
        parameterMap.put("orientation", 0.0);
        final Product targetPoduct = createReprojectedProduct();
        
        assertNotNull(targetPoduct);
        final GeoPos geoPos = targetPoduct.getGeoCoding().getGeoPos(new PixelPos(0.5f, 0.5f), null);
        assertEquals(new GeoPos(52.0f, 9.0f), geoPos);
        testPixelValue(targetPoduct, 23.5f, 13.5f, 299, 1.0e-6);
    }

    @Test
    public void testIncludeTiePointGrids() throws Exception {
        parameterMap.put("crsCode", WGS84_CODE);
        Product targetPoduct = createReprojectedProduct();
        
        TiePointGrid[] tiePointGrids = targetPoduct.getTiePointGrids();
        assertNotNull(tiePointGrids);
        assertEquals(0, tiePointGrids.length);
        Band latGrid = targetPoduct.getBand("latGrid");
        assertNotNull(latGrid);
        
        parameterMap.put("includeTiePointGrids", false);
        targetPoduct = createReprojectedProduct();
        tiePointGrids = targetPoduct.getTiePointGrids();
        assertNotNull(tiePointGrids);
        assertEquals(0, tiePointGrids.length);
        latGrid = targetPoduct.getBand("latGrid");
        assertNull(latGrid);
    }


    // Expected failure cases 
    @Test(expected = OperatorException.class)
    public void testEmptyParameterMap() {
        createReprojectedProduct();
    }

    @Test(expected = OperatorException.class)
    public void testParameterAmbigouity_wkt_epsgCode() {
        parameterMap.put("wkt", UTM33N_WKT);
        parameterMap.put("crsCode", UTM33N_CODE);
        createReprojectedProduct();
    }

    @Test(expected = OperatorException.class)
    public void testParameterAmbigouity_wkt_wktFile() {
        parameterMap.put("wkt", UTM33N_WKT);
        parameterMap.put("wktFile", wktFile);
        createReprojectedProduct();
    }

    @Test(expected = OperatorException.class)
    public void testParameterAmbigouity_wkt_collocateProduct() {
        final Map<String, Product> productMap = new HashMap<String, Product>(5);
        productMap.put("source", sourceProduct);
        productMap.put("collocate", sourceProduct);
        parameterMap.put("wkt", UTM33N_WKT);
        createReprojectedProduct(productMap);
    }

    @Test(expected = OperatorException.class)
    public void testUnknownResamplingMethode() {
        parameterMap.put("resampling", "Super_Duper_Resampling");
        createReprojectedProduct();
    }

    @Test(expected = OperatorException.class)
    public void testMissingPixelSizeY() {
        parameterMap.put("pixelSizeX", 0.024);
        createReprojectedProduct();
    }

    @Test(expected = OperatorException.class)
    public void testMissingPixelSizeX() {
        parameterMap.put("pixelSizeY", 0.024);
        createReprojectedProduct();
    }

    @Test(expected = OperatorException.class)
    public void testMissingReferencingPixelX() {
        parameterMap.put("referencePixelY", 0.5);
        parameterMap.put("easting", 1234.5);
        parameterMap.put("northing", 1234.5);
        createReprojectedProduct();
    }

    @Test(expected = OperatorException.class)
    public void testMissingReferencingpixelY() {
        parameterMap.put("referencePixelX", 0.5);
        parameterMap.put("easting", 1234.5);
        parameterMap.put("northing", 1234.5);
        createReprojectedProduct();
    }


    @Test(expected = OperatorException.class)
    public void testMissingReferencingNorthing() {
        parameterMap.put("referencePixelX", 0.5);
        parameterMap.put("referencePixelY", 0.5);
        parameterMap.put("easting", 1234.5);
        createReprojectedProduct();
    }

    @Test(expected = OperatorException.class)
    public void testMissingReferencingEasting() {
        parameterMap.put("referencePixelX", 0.5);
        parameterMap.put("referencePixelY", 0.5);
        parameterMap.put("northing", 1234.5);
        createReprojectedProduct();
    }

}
