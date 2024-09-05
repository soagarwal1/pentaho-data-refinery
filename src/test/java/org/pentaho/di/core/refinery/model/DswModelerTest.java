/*! ******************************************************************************
 *
 * Pentaho Community Edition Project: data-refinery-pdi-plugin
 *
 * Copyright (C) 2002-2024 by Hitachi Vantara : http://www.pentaho.com
 *
 * *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ********************************************************************************/

package org.pentaho.di.core.refinery.model;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pentaho.agilebi.modeler.ModelerException;
import org.pentaho.agilebi.modeler.ModelerWorkspace;
import org.pentaho.agilebi.modeler.geo.GeoContextPropertiesProvider;
import org.pentaho.agilebi.modeler.models.annotations.AnnotationType;
import org.pentaho.agilebi.modeler.models.annotations.CreateAttribute;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotation;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotation.GeoType;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;
import org.pentaho.agilebi.modeler.util.TableModelerSource;
import org.pentaho.di.core.KettleClientEnvironment;
import org.pentaho.di.core.Props;
import org.pentaho.di.core.database.Database;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.plugins.PluginRegistry;
import org.pentaho.di.core.plugins.StepPluginType;
import org.pentaho.di.core.refinery.model.DswModeler.ColumnMismatchException;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.value.ValueMetaInteger;
import org.pentaho.di.core.row.value.ValueMetaNumber;
import org.pentaho.di.core.row.value.ValueMetaString;
import org.pentaho.di.trans.step.StepMetaDataCombi;
import org.pentaho.di.trans.steps.tableoutput.TableOutputData;
import org.pentaho.di.trans.steps.tableoutput.TableOutputMeta;
import org.pentaho.metadata.automodel.PhysicalTableImporter;
import org.pentaho.metadata.model.Domain;
import org.pentaho.metadata.model.IPhysicalModel;
import org.pentaho.metadata.model.LogicalModel;
import org.pentaho.metadata.model.SqlDataSource;
import org.pentaho.metadata.model.SqlPhysicalModel;
import org.pentaho.metadata.model.olap.OlapAnnotation;
import org.pentaho.metadata.model.olap.OlapCube;
import org.pentaho.metadata.model.olap.OlapDimension;
import org.pentaho.metadata.model.olap.OlapHierarchy;
import org.pentaho.metadata.model.olap.OlapHierarchyLevel;
import org.pentaho.metadata.util.XmiParser;
import org.pentaho.metastore.api.IMetaStore;
import org.pentaho.metastore.stores.memory.MemoryMetaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class DswModelerTest {

  private IMetaStore metaStore;

  @Before
  public void setUp() throws Exception {
    metaStore = new MemoryMetaStore();
  }

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    if ( !KettleClientEnvironment.isInitialized() ) {
      KettleClientEnvironment.init();
    }
    PluginRegistry.addPluginType( StepPluginType.getInstance() );
    PluginRegistry.init();
    if ( !Props.isInitialized() ) {
      Props.init( 0 );
    }
  }

  @Test
  public void testUpdateModel() throws Exception {
    InputStream in = getClass().getResourceAsStream( "/SimpleModel.xmi" );
    try {
      XmiParser xmiParser = new XmiParser();
      Domain existing = xmiParser.parseXmi( in );
      DatabaseMeta dbMeta = createOrderfactDB();
      DswModeler modeler = new DswModeler( null );
      Domain newModel = modeler.updateModel( "NewModelName", existing, dbMeta, "", "orderfact" );

      String xmi = xmiParser.generateXmi( newModel );
      assertTrue( "new name should be in there", xmi.contains( "NewModelName" ) );
      assertFalse( "old name shouldn't be there", xmi.contains( "SimpleModel" ) );
      LogicalModel analysisModel = newModel.getLogicalModels().get( 1 );
      @SuppressWarnings( "unchecked" )
      List<OlapDimension> dims = (List<OlapDimension>) analysisModel.getProperty( LogicalModel.PROPERTY_OLAP_DIMS );
      boolean foundTime = false;
      for ( OlapDimension dim : dims ) {
        if ( dim.getName().equals( "Time" ) ) {
          foundTime = true;
          assertEquals( 1, dim.getHierarchies().size() );
          OlapHierarchy timeHier = dim.getHierarchies().get( 0 );
          assertEquals( "Year", timeHier.getHierarchyLevels().get( 0 ).getName() );
          assertEquals( "Quarter", timeHier.getHierarchyLevels().get( 1 ).getName() );
          assertEquals( "Month", timeHier.getHierarchyLevels().get( 2 ).getName() );
        }
      }
      assertTrue( foundTime );
    } finally {
      IOUtils.closeQuietly( in );
    }
  }

  @Test
  public void testUpdateModelMissingFields() throws Exception {
    InputStream in = getClass().getResourceAsStream( "/SimpleModel.xmi" );
    try {
      XmiParser xmiParser = new XmiParser();
      Domain existing = xmiParser.parseXmi( in );
      DatabaseMeta dbMeta = createLimpOrderfactDB();
      DswModeler modeler = new DswModeler( null );
      modeler.updateModel( "NewModelName", existing, dbMeta, "", "orderfact" );
      fail( "no exception" );
    } catch ( ColumnMismatchException e ) {
      // ok
    } finally {
      IOUtils.closeQuietly( in );
    }
  }

  @Test
  public void testUpdateModelWrongType() throws Exception {
    InputStream in = getClass().getResourceAsStream( "/SimpleModel.xmi" );
    try {
      XmiParser xmiParser = new XmiParser();
      Domain existing = xmiParser.parseXmi( in );
      DatabaseMeta dbMeta = createWrongTypeOrderfactDB();
      DswModeler modeler = new DswModeler( null );
      modeler.updateModel( "NewModelName", existing, dbMeta, "", "orderfact" );
      fail( "no exception" );
    } catch ( ColumnMismatchException e ) {
      // ok
    } finally {
      IOUtils.closeQuietly( in );
    }
  }

  @Test
  public void testCreateModel() throws Exception {
    DatabaseMeta dbMeta = createOrderfactDB();
    TableModelerSource source = new TableModelerSource( dbMeta, "orderfact", "" );

    final LogChannelInterface log = mock( LogChannelInterface.class );
    when( log.isBasic() ).thenReturn( true );
    DswModeler modeler = new DswModeler( log );
    PhysicalTableImporter.ImportStrategy importStrategy = getImportStrategy();

    ModelAnnotation annotation1 = whenApply( "summary 1" );
    ModelAnnotation annotation2 = whenApply( "summary 2" );

    ModelAnnotationGroup modelAnnotations = new ModelAnnotationGroup( annotation1, annotation2 );
    Domain dsw = modeler.createModel( "FromScratch", source, dbMeta, importStrategy, modelAnnotations, metaStore );
    LogicalModel anlModel = dsw.getLogicalModels().get( 1 );
    LogicalModel rptModel = dsw.getLogicalModels().get( 0 );
    @SuppressWarnings( "unchecked" )
    OlapCube cube = ( (List<OlapCube>) anlModel.getProperty( LogicalModel.PROPERTY_OLAP_CUBES ) ).get( 0 );

    //only fields in rowMeta should be present
    assertEquals( 2, cube.getOlapMeasures().size() );
    assertEquals( "Quantity Ordered", cube.getOlapMeasures().get( 0 ).getName() );
    assertEquals( "Total Price", cube.getOlapMeasures().get( 1 ).getName() );
    assertEquals( 4, cube.getOlapDimensionUsages().size() );
    // categoryName
    assertEquals( "FromScratch", rptModel.getCategories().get( 0 ).getName( "en_US" ) );
    verify( annotation1 ).apply( any( ModelerWorkspace.class ), same( metaStore ) );
    verify( log ).logBasic( "Successfully applied annotation: summary 1" );
    verify( annotation2 ).apply( any( ModelerWorkspace.class ), same( metaStore ) );
    verify( log ).logBasic( "Successfully applied annotation: summary 2" );
  }

  @Test
  public void testCreateModelNoJndi() throws Exception {
    DatabaseMeta dbMeta = createOrderfactDB();
    TableModelerSource source = new TableModelerSource( dbMeta, "orderfact", "" );

    final LogChannelInterface log = mock( LogChannelInterface.class );
    when( log.isBasic() ).thenReturn( true );
    DswModeler modeler = new DswModeler( log );
    modeler.setUseJndi( false );
    PhysicalTableImporter.ImportStrategy importStrategy = getImportStrategy();

    Domain dsw =
      modeler.createModel( "FromScratch", source, dbMeta, importStrategy, new ModelAnnotationGroup(), metaStore );
    assertEquals( SqlDataSource.DataSourceType.NATIVE,
      ( (SqlPhysicalModel) dsw.getPhysicalModels().get( 0 ) ).getDatasource().getType() );
  }
  @Test
  public void testNullAnnotationsAreIgnored() throws Exception {
    DatabaseMeta dbMeta = createOrderfactDB();
    TableModelerSource source = new TableModelerSource( dbMeta, "orderfact", "" );

    final LogChannelInterface log = mock( LogChannelInterface.class );
    when( log.isBasic() ).thenReturn( true );
    DswModeler modeler = new DswModeler( log );
    PhysicalTableImporter.ImportStrategy importStrategy = getImportStrategy();

    ModelAnnotation modelAnnotation = new ModelAnnotation();
    ModelAnnotationGroup modelAnnotations = new ModelAnnotationGroup( modelAnnotation );

    //no expections here is the test
    modeler.createModel( "FromScratch", source, dbMeta, importStrategy, modelAnnotations, metaStore );
    verify( log ).logDebug( "Ignoring a null annotation" );
  }

  private ModelAnnotation whenApply( final String summary ) throws org.pentaho.agilebi.modeler.ModelerException {
    ModelAnnotation annotation = mock( ModelAnnotation.class );
    when( annotation.apply( any( ModelerWorkspace.class ), same( metaStore ) ) ).thenReturn( true );
    whenSummary( annotation, summary );
    return annotation;
  }

  private void whenSummary( final ModelAnnotation annotation, final String summary ) {
    AnnotationType annotationType1 = mock( AnnotationType.class );
    when( annotation.getAnnotation() ).thenReturn( annotationType1 );
    when( annotationType1.getSummary() ).thenReturn( summary );
  }

  @Test
  public void testAnnotationsAreRetriedUntilDone() throws Exception {
    DatabaseMeta dbMeta = createOrderfactDB();
    TableModelerSource source = new TableModelerSource( dbMeta, "orderfact", "" );
    final LogChannelInterface log = mock( LogChannelInterface.class );
    when( log.isBasic() ).thenReturn( false );
    DswModeler modeler = new DswModeler( log );

    PhysicalTableImporter.ImportStrategy importStrategy = getImportStrategy();
    ModelAnnotation annotation1 = mock( ModelAnnotation.class );
    ModelAnnotation annotation2 = mock( ModelAnnotation.class );
    ModelAnnotationGroup modelAnnotations = new ModelAnnotationGroup( annotation1, annotation2 );
    when( annotation1.apply( any( ModelerWorkspace.class ), same( metaStore ) ) ).thenReturn( false, true );
    when( annotation2.apply( any( ModelerWorkspace.class ), same( metaStore ) ) ).thenReturn( true );
    AnnotationType annotationType = mock( AnnotationType.class );
    when( annotation1.getAnnotation() ).thenReturn( annotationType );
    when( annotation2.getAnnotation() ).thenReturn( annotationType );

    Domain dsw = modeler.createModel( "FromScratch", source, dbMeta, importStrategy, modelAnnotations, metaStore );
    LogicalModel anlModel = dsw.getLogicalModels().get( 1 );
    LogicalModel rptModel = dsw.getLogicalModels().get( 0 );
    @SuppressWarnings( "unchecked" )
    OlapCube cube = ( (List<OlapCube>) anlModel.getProperty( LogicalModel.PROPERTY_OLAP_CUBES ) ).get( 0 );

    //only fields in rowMeta should be present
    assertEquals( 2, cube.getOlapMeasures().size() );
    assertEquals( "Quantity Ordered", cube.getOlapMeasures().get( 0 ).getName() );
    assertEquals( "Total Price", cube.getOlapMeasures().get( 1 ).getName() );
    assertEquals( 4, cube.getOlapDimensionUsages().size() );
    // categoryName
    assertEquals( "FromScratch", rptModel.getCategories().get( 0 ).getName( "en_US" ) );
    verify( annotation1, times( 2 ) ).apply( any( ModelerWorkspace.class ), same( metaStore ) );
    verify( log, never() ).logBasic( any( String.class ) );
    verify( annotation2 ).apply( any( ModelerWorkspace.class ), same( metaStore ) );
    verify( log, never() ).logBasic( any( String.class ) );
  }

  @Test
  public void testAnnotationsStopRetryWhenAllFailed() throws Exception {
    DatabaseMeta dbMeta = createOrderfactDB();
    TableModelerSource source = new TableModelerSource( dbMeta, "orderfact", "" );
    final LogChannelInterface log = mock( LogChannelInterface.class );
    when( log.isBasic() ).thenReturn( true );
    DswModeler modeler = new DswModeler( null );
    modeler.setLog( log );

    PhysicalTableImporter.ImportStrategy importStrategy = getImportStrategy();
    ModelAnnotation annotation1 = mock( ModelAnnotation.class );
    whenSummary( annotation1, "annotation 1 Summary" );
    ModelAnnotation annotation2 = mock( ModelAnnotation.class );
    whenSummary( annotation2, "annotation 2 Summary" );
    ModelAnnotation annotation3 = mock( ModelAnnotation.class );
    whenSummary( annotation3, "annotation 3 Summary" );
    ModelAnnotationGroup modelAnnotations = new ModelAnnotationGroup( annotation1, annotation2, annotation3 );
    when( annotation1.apply( any( ModelerWorkspace.class ), same( metaStore ) ) ).thenReturn( false, false );
    when( annotation2.apply( any( ModelerWorkspace.class ), same( metaStore ) ) ).thenReturn( false, false );
    when( annotation3.apply( any( ModelerWorkspace.class ), same( metaStore ) ) ).thenReturn( true );

    Domain dsw = modeler.createModel( "FromScratch", source, dbMeta, importStrategy, modelAnnotations, metaStore );
    LogicalModel anlModel = dsw.getLogicalModels().get( 1 );
    LogicalModel rptModel = dsw.getLogicalModels().get( 0 );
    @SuppressWarnings( "unchecked" )
    OlapCube cube = ( (List<OlapCube>) anlModel.getProperty( LogicalModel.PROPERTY_OLAP_CUBES ) ).get( 0 );

    //only fields in rowMeta should be present
    assertEquals( 2, cube.getOlapMeasures().size() );
    assertEquals( "Quantity Ordered", cube.getOlapMeasures().get( 0 ).getName() );
    assertEquals( "Total Price", cube.getOlapMeasures().get( 1 ).getName() );
    assertEquals( 4, cube.getOlapDimensionUsages().size() );
    // categoryName
    assertEquals( "FromScratch", rptModel.getCategories().get( 0 ).getName( "en_US" ) );
    verify( annotation1, times( 2 ) ).apply( any( ModelerWorkspace.class ), same( metaStore ) );
    verify( log ).logBasic( "Unable to apply annotation: annotation 1 Summary" );
    verify( annotation2, times( 2 ) ).apply( any( ModelerWorkspace.class ), same( metaStore ) );
    verify( log ).logBasic( "Unable to apply annotation: annotation 2 Summary" );
    verify( annotation3 ).apply( any( ModelerWorkspace.class ), same( metaStore ) );
    verify( log ).logBasic( "Successfully applied annotation: annotation 3 Summary" );
  }

  @Test
  public void testCreateWithEmptyStreamDoesNotCreateModel() throws Exception {
    DatabaseMeta dbMeta = createOrderfactDB();
    TableModelerSource source = new TableModelerSource( dbMeta, "orderfact", "" );
    final LogChannelInterface log = mock( LogChannelInterface.class );
    DswModeler modeler = new DswModeler( log );

    PhysicalTableImporter.ImportStrategy importStrategy = getEmptyMetaImportStrategy();
    ModelAnnotation annotation1 = mock( ModelAnnotation.class );
    ModelAnnotation annotation2 = mock( ModelAnnotation.class );
    ModelAnnotationGroup modelAnnotations = new ModelAnnotationGroup( annotation1, annotation2 );
    try {
      modeler.createModel( "FromScratch", source, dbMeta, importStrategy, modelAnnotations, metaStore );
    } catch ( ModelerException e ) {
      assertEquals( "No Data to Model", e.getMessage() );
    }
  }

  @Test
  public void testGeoContextInitialized() throws Exception {
    DatabaseMeta dbMeta = createOrderfactDB();
    TableModelerSource source = new TableModelerSource( dbMeta, "orderfact", "" );

    final LogChannelInterface log = mock( LogChannelInterface.class );
    when( log.isBasic() ).thenReturn( true );
    DswModeler modeler = new DswModeler( log );
    initTestGeoContextProvider( modeler );

    PhysicalTableImporter.ImportStrategy importStrategy = getImportStrategy();

    CreateAttribute geoAttribute = new CreateAttribute();
    geoAttribute.setName( "State" );
    geoAttribute.setGeoType( ModelAnnotation.GeoType.State );
    geoAttribute.setDimension( "geo" );
    geoAttribute.setField( "Status" );
    ModelAnnotation annotation1 = new ModelAnnotation<CreateAttribute>( geoAttribute );
    ModelAnnotationGroup modelAnnotations = new ModelAnnotationGroup( annotation1 );
    Domain dsw = modeler.createModel( "FromScratch", source, dbMeta, importStrategy, modelAnnotations, metaStore );
    LogicalModel anlModel = dsw.getLogicalModels().get( 1 );
    @SuppressWarnings( "unchecked" )
    OlapCube cube = ( (List<OlapCube>) anlModel.getProperty( LogicalModel.PROPERTY_OLAP_CUBES ) ).get( 0 );
    assertEquals( 4, cube.getOlapDimensionUsages().size() );
    List<OlapAnnotation> olapAnnotations =
        cube.getOlapDimensionUsages().get( 3 ).getOlapDimension().getHierarchies().get( 0 ).getHierarchyLevels()
            .get( 0 ).getAnnotations();
    assertEquals( "Data.Role", olapAnnotations.get( 0 ).getName() );
    assertEquals( "Geography", olapAnnotations.get( 0 ).getValue() );
    assertEquals( "Geo.Role", olapAnnotations.get( 1 ).getName() );
    assertEquals( "state", olapAnnotations.get( 1 ).getValue() );
    assertEquals( "Geo.RequiredParents", olapAnnotations.get( 2 ).getName() );
    assertEquals( "country", olapAnnotations.get( 2 ).getValue() );
    assertEquals( 3, olapAnnotations.size() );
  }

  private void initTestGeoContextProvider( DswModeler modeler ) throws IOException {
    String path = getClass().getResource( "/geoRoles.properties" ).getPath();
    FileInputStream fis = new FileInputStream( path );
    Properties props = new Properties();
    props.load( fis );
    modeler.setGeoContextConfigProvider( new GeoContextPropertiesProvider( props ) );
  }

  @Test
  public void testAutoGeoGetsApplied() throws Exception {
    DatabaseMeta dbMeta = createGeoTable();
    TableModelerSource source = new TableModelerSource( dbMeta, "geodata", "" );

    final LogChannelInterface log = mock( LogChannelInterface.class );
    when( log.isBasic() ).thenReturn( true );
    DswModeler modeler = new DswModeler( log );
    initTestGeoContextProvider( modeler );
    PhysicalTableImporter.ImportStrategy importStrategy = getGeoStepMetaDataCombi();

    Domain dsw = modeler.createModel(
        "FromScratch", source, dbMeta, importStrategy, new ModelAnnotationGroup( ), metaStore );
    LogicalModel anlModel = dsw.getLogicalModels().get( 1 );
    @SuppressWarnings( "unchecked" )
    OlapCube cube = ( (List<OlapCube>) anlModel.getProperty( LogicalModel.PROPERTY_OLAP_CUBES ) ).get( 0 );
    assertEquals( 3, cube.getOlapDimensionUsages().size() );
    List<OlapHierarchyLevel> hierarchyLevels =
        cube.getOlapDimensionUsages().get( 0 ).getOlapDimension().getHierarchies().get( 0 ).getHierarchyLevels();
    assertGeoAnnotations( hierarchyLevels.get( 0 ).getAnnotations(), "state", "country" );
    assertGeoAnnotations( hierarchyLevels.get( 1 ).getAnnotations(), "county", "country,state" );
    assertGeoAnnotations( hierarchyLevels.get( 2 ).getAnnotations(), "city", "country,state" );
    assertGeoAnnotations( hierarchyLevels.get( 3 ).getAnnotations(), "postal_code", "country" );
  }

  private void assertGeoAnnotations( final List<OlapAnnotation> olapAnnotations, final String geoRole,
                                     final String requiredParents ) {
    assertEquals( "Data.Role", olapAnnotations.get( 0 ).getName() );
    assertEquals( "Geography", olapAnnotations.get( 0 ).getValue() );
    assertEquals( "Geo.Role", olapAnnotations.get( 1 ).getName() );
    assertEquals( geoRole, olapAnnotations.get( 1 ).getValue() );
    assertEquals( "Geo.RequiredParents", olapAnnotations.get( 2 ).getName() );
    assertEquals( requiredParents, olapAnnotations.get( 2 ).getValue() );
    assertEquals( 3, olapAnnotations.size() );
  }

  @Test
  public void testGeoConflict() throws Exception {
    DatabaseMeta dbMeta = createGeoTable();
    TableModelerSource source = new TableModelerSource( dbMeta, "geodata", "" );

    final LogChannelInterface log = mock( LogChannelInterface.class );
    when( log.isBasic() ).thenReturn( false );
    DswModeler modeler = new DswModeler( log );

    initTestGeoContextProvider( modeler );

    final String geoDim = "Geography";
    CreateAttribute state = new CreateAttribute();
    state.setName( "State" );
    state.setDimension( geoDim );
    state.setHierarchy( geoDim );
    state.setGeoType( GeoType.State );
    state.setField( "state" );

    CreateAttribute zipCode = new CreateAttribute();
    zipCode.setName( "ZipCode" );
    zipCode.setDimension( geoDim );
    zipCode.setHierarchy( geoDim );
    zipCode.setGeoType( GeoType.Postal_Code );
    zipCode.setParentAttribute( "State" );
    zipCode.setField( "zipcode" );

    CreateAttribute county = new CreateAttribute();
    county.setName( "County" );
    county.setDimension( geoDim );
    county.setHierarchy( geoDim );
    county.setGeoType( GeoType.County );
    county.setParentAttribute( "ZipCode" );
    county.setField( "county" );

    CreateAttribute city = new CreateAttribute();
    city.setName( "City" );
    city.setDimension( geoDim );
    city.setHierarchy( geoDim );
    city.setGeoType( GeoType.City );
    city.setParentAttribute( "County" );
    city.setField( "city" );

    // have root level state as last attribute
    ModelAnnotationGroup modelAnnotations = new ModelAnnotationGroup(
      new ModelAnnotation<CreateAttribute>( zipCode ),
      new ModelAnnotation<CreateAttribute>( county ),
      new ModelAnnotation<CreateAttribute>( city ),
      new ModelAnnotation<CreateAttribute>( state )
    );
    modelAnnotations.setName( "GeoDim" );
    PhysicalTableImporter.ImportStrategy importStrategy = getGeoStepMetaDataCombi();

    Domain domain = modeler.createModel(
        "GeographyDimModel", source, dbMeta, importStrategy, modelAnnotations, metaStore );
    boolean foundDim = false;
    for ( OlapDimension dim : getDimensions( domain ) ) {
      if ( dim.getName().equals( geoDim ) ) {
        foundDim = true;
        assertEquals( "one hierarchy.", 1, dim.getHierarchies().size() );
        OlapHierarchy geoh = dim.getHierarchies().get( 0 );
        assertEquals( "all annotations modeled.", modelAnnotations.size(), geoh.getHierarchyLevels().size() );
        assertEquals( state.getName(), geoh.getHierarchyLevels().get( 0 ).getName() );
        assertEquals( zipCode.getName(), geoh.getHierarchyLevels().get( 1 ).getName() );
        assertEquals( county.getName(), geoh.getHierarchyLevels().get( 2 ).getName() );
        assertEquals( city.getName(), geoh.getHierarchyLevels().get( 3 ).getName() );
        break;
      }
    }
    assertTrue( geoDim + " modeled.", foundDim );
  }

  private PhysicalTableImporter.ImportStrategy getImportStrategy() throws ModelerException {
    StepMetaDataCombi stepMetaDataCombi = new StepMetaDataCombi();
    RowMeta rowMeta = new RowMeta();
    rowMeta.addValueMeta( new ValueMetaInteger( "QuantityOrdered" ) );
    rowMeta.addValueMeta( new ValueMetaNumber( "TotalPrice" ) );
    rowMeta.addValueMeta( new ValueMetaString( "ProductCode" ) );
    rowMeta.addValueMeta( new ValueMetaString( "Status" ) );
    rowMeta.addValueMeta( new ValueMetaString( "unknownfield" ) );
    TableOutputData tableOutputData = new TableOutputData();
    tableOutputData.insertRowMeta = rowMeta;
    stepMetaDataCombi.data = tableOutputData;
    TableOutputMeta tableOutputMeta = new TableOutputMeta();
    tableOutputMeta.setSpecifyFields( true );
    tableOutputMeta.setFieldStream( new String[] { "Quantity Ordered", "Total Price", "Product Code", "Status" } );
    tableOutputMeta.setFieldDatabase( new String[] { "QuantityOrdered", "TotalPrice", "ProductCode", "Status" } );
    stepMetaDataCombi.meta = tableOutputMeta;
    return new RefineryValueMetaStrategy( stepMetaDataCombi );
  }

  private PhysicalTableImporter.ImportStrategy getGeoStepMetaDataCombi() throws ModelerException {
    StepMetaDataCombi stepMetaDataCombi = new StepMetaDataCombi();
    RowMeta rowMeta = new RowMeta();
    rowMeta.addValueMeta( new ValueMetaInteger( "state_fips" ) );
    rowMeta.addValueMeta( new ValueMetaString( "state" ) );
    rowMeta.addValueMeta( new ValueMetaString( "state_abbr" ) );
    rowMeta.addValueMeta( new ValueMetaString( "zipcode" ) );
    rowMeta.addValueMeta( new ValueMetaString( "county" ) );
    rowMeta.addValueMeta( new ValueMetaString( "city" ) );
    TableOutputData tableOutputData = new TableOutputData();
    tableOutputData.insertRowMeta = rowMeta;
    stepMetaDataCombi.data = tableOutputData;
    TableOutputMeta tableOutputMeta = new TableOutputMeta();
    tableOutputMeta.setFieldStream( new String[] { "state_fips", "state", "state_abbr", "zipcode", "county", "city" } );
    tableOutputMeta.setFieldDatabase( new String[] { "state_fips", "state", "state_abbr", "zipcode", "county", "city" } );
    stepMetaDataCombi.meta = tableOutputMeta;
    return new RefineryValueMetaStrategy( stepMetaDataCombi );
  }

  private PhysicalTableImporter.ImportStrategy getEmptyMetaImportStrategy() throws ModelerException {
    StepMetaDataCombi stepMetaDataCombi = new StepMetaDataCombi();
    TableOutputData tableOutputData = new TableOutputData();
    stepMetaDataCombi.data = tableOutputData;
    TableOutputMeta tableOutputMeta = new TableOutputMeta();
    tableOutputMeta.setFieldDatabase( new String[] { "QuantityOrdered", "TotalPrice", "ProductCode", "Status" } );
    stepMetaDataCombi.meta = tableOutputMeta;
    return new RefineryValueMetaStrategy( stepMetaDataCombi );
  }

  private DatabaseMeta createOrderfactDB() throws Exception {
    DatabaseMeta dbMeta = newH2Db();
    Database db = new Database( null, dbMeta );
    db.connect();
    db.execStatement( "DROP TABLE IF EXISTS orderfact;" );
    db.execStatement( "CREATE TABLE orderfact\n"
        + "(\n"
        + "   ordernumber int,\n"
        + "   productcode varchar(50),\n"
        + "   quantityordered int,\n"
        + "   priceeach decimal(31,7),\n"
        + "   orderlinenumber int,\n"
        + "   totalprice double,\n"
        + "   status varchar(15),\n"
        + "   time_id varchar(10),\n"
        + "   qtr_id bigint,\n"
        + "   month_id bigint,\n"
        + "   year_id bigint\n"
        + ");\n" );
    db.disconnect();
    return dbMeta;
  }

  private DatabaseMeta createLimpOrderfactDB() throws Exception {
    DatabaseMeta dbMeta = newH2Db();
    Database db = new Database( null, dbMeta );
    db.connect();
    db.execStatement( "DROP TABLE IF EXISTS orderfact;" );
    db.execStatement( "CREATE TABLE orderfact\n"
        + "(\n"
        + "   ordernumber int,\n"
        + "   productcode varchar(50),\n"
        + "   quantityordered int,\n"
        + "   priceeach decimal(31,7),\n"
        // no orderlinenumber
        + "   totalprice double,\n"
        // no status
        + "   time_id varchar(10),\n"
        + "   qtr_id bigint,\n"
        + "   month_id bigint,\n"
        + "   year_id bigint\n"
        + ");\n" );
    db.disconnect();
    return dbMeta;
  }

  private DatabaseMeta createWrongTypeOrderfactDB() throws Exception {
    DatabaseMeta dbMeta = newH2Db();
    Database db = new Database( null, dbMeta );
    db.connect();
    db.execStatement( "DROP TABLE IF EXISTS orderfact;" );
    db.execStatement( "CREATE TABLE orderfact\n"
        + "(\n"
        + "   ordernumber varchar(10),\n" // not int
        + "   productcode varchar(50),\n"
        + "   quantityordered int,\n"
        + "   priceeach decimal(31,7),\n"
        + "   orderlinenumber int,\n"
        + "   totalprice double,\n"
        + "   status varchar(15),\n"
        + "   time_id varchar(10),\n"
        + "   qtr_id bigint,\n"
        + "   month_id bigint,\n"
        + "   year_id bigint\n"
        + ");\n" );
    db.disconnect();
    return dbMeta;
  }

  private DatabaseMeta createGeoTable() throws Exception {
    DatabaseMeta dbMeta = newH2Db();
    Database db = new Database( null, dbMeta );
    db.connect();
    db.execStatement( "DROP TABLE if exists geodata;" );
    db.execStatement( "CREATE TABLE geodata\n"
        + "(\n"
        + "  state_fips bigint\n"
        + ", state varchar(25)\n"
        + ", state_abbr varchar(4)\n"
        + ", zipcode varchar(10)\n"
        + ", county varchar(45)\n"
        + ", city varchar(45)\n"
        + ");\n" );
    db.disconnect();
    return dbMeta;

  }

  private DatabaseMeta newH2Db() {
    // DB Setup
    String dbDir = "./target/test-db/DswModelerTest-H2-DB";
    File file = new File( dbDir + ".h2.db" );
    if ( file.exists() ) {
      file.delete();
    }
    DatabaseMeta dbMeta = new DatabaseMeta( "myh2", "H2", "Native", null, dbDir, null, "sa", null );
    return dbMeta;
  }

  @Test
  public void testUpdateDatasourceAccess() throws Exception {

    DatabaseMeta databaseMeta = new DatabaseMeta();
    databaseMeta.setName( "connectionName" );

    SqlDataSource sqlDataSource = new SqlDataSource();
    sqlDataSource.setDatabaseName( "databaseName" );
    sqlDataSource.setType( SqlDataSource.DataSourceType.NATIVE );

    SqlPhysicalModel physicalModel = new SqlPhysicalModel();
    physicalModel.setDatasource( sqlDataSource );

    List<IPhysicalModel> physicalModels = new ArrayList<IPhysicalModel>();
    physicalModels.add( physicalModel );

    Domain domain = mock( Domain.class );
    when( domain.getPhysicalModels() ).thenReturn( physicalModels );

    DswModeler modeler = new DswModeler( null );
    modeler.updateDatasourceAccess( domain, databaseMeta );

    // Assert data source updated
    assertTrue( sqlDataSource.getType() == SqlDataSource.DataSourceType.JNDI );
    assertTrue( sqlDataSource.getDatabaseName().equals( databaseMeta.getName() ) );

    // test null
    physicalModel.setDatasource( null );
    modeler.updateDatasourceAccess( domain, databaseMeta );
  }

  @SuppressWarnings( "unchecked" )
  private List<OlapDimension> getDimensions( Domain domain ) {
    return (List<OlapDimension>) domain.getLogicalModels().get( 1 ).getProperty(
        LogicalModel.PROPERTY_OLAP_DIMS );
  }
}
