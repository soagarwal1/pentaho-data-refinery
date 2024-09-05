/*
 * ******************************************************************************
 *
 * Copyright (C) 2002-2024 by Hitachi Vantara : http://www.pentaho.com
 *
 * ******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pentaho.di.core.refinery.model;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.pentaho.agilebi.modeler.ModelerException;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by rfellows on 11/24/15.
 */
@RunWith( MockitoJUnitRunner.class )
public class GeoContextBlueprintConfigProviderTest {

  GeoContextBlueprintConfigProvider geoContextProvider;
  @Mock BundleContext bundleContext;
  @Mock ServiceReference serviceRef;
  @Mock ConfigurationAdmin configAdmin;
  @Mock Configuration configuration;

  @Before
  public void setUp() throws Exception {
    geoContextProvider = new GeoContextBlueprintConfigProvider();
  }

  @Test
  public void testGetDimensionName_noProps() throws Exception {
    geoContextProvider.props = new Hashtable<>();
    assertNull( geoContextProvider.getDimensionName() );
  }

  @Test
  public void testGetDimensionName() throws Exception {
    geoContextProvider.props.put( "geo.dimension.name", "DIM NAME" );

    assertEquals( "DIM NAME", geoContextProvider.getDimensionName() );
  }

  @Test
  public void testGetRoles_noProps() throws Exception {
    geoContextProvider.props = new Hashtable<>();
    assertNull( geoContextProvider.getRoles() );
  }

  @Test
  public void testGetRoles() throws Exception {
    geoContextProvider.props.put( "geo.roles", "country, state, city" );

    assertEquals( "country, state, city", geoContextProvider.getRoles() );
  }

  @Test
  public void testGetRoleAliases() throws Exception {
    geoContextProvider.props.put( "geo.country.aliases", "CTRY, CTR" );

    assertEquals( "CTRY, CTR", geoContextProvider.getRoleAliases( "country" ) );
  }

  @Test( expected = ModelerException.class )
  public void testGetRoleAliases_noMatch() throws Exception {
    geoContextProvider.getRoleAliases( "country" );
  }

  @Test
  public void testGetRoleRequirements() throws Exception {
    geoContextProvider.props.put( "geo.state.required-parents", "country" );

    assertEquals( "country", geoContextProvider.getRoleRequirements( "state" ) );
  }

  @Test
  public void testGetRoleRequirements_noMatch() throws Exception {
    assertNull( geoContextProvider.getRoleRequirements( "state" ) );
  }

  @Test
  public void testInitProps() throws Exception {
    Dictionary<String, Object> testProps = new Hashtable<>();
    testProps.put( "geo.roles", "country, state, city" );

    when( bundleContext.getServiceReference( anyString() ) ).thenReturn( serviceRef );
    when( bundleContext.getService( serviceRef ) ).thenReturn( configAdmin );
    when( configAdmin.getConfiguration( "pentaho.geo.roles" ) ).thenReturn( configuration );
    when( configuration.getProperties() ).thenReturn( testProps );

    geoContextProvider.setBundleContext( bundleContext );

    assertEquals( testProps, geoContextProvider.props );
  }

  @Test
  public void testInitProps_exeptionWhenGettingBlueprintConfig() throws Exception {

    when( bundleContext.getServiceReference( anyString() ) ).thenReturn( serviceRef );
    when( bundleContext.getService( serviceRef ) ).thenReturn( configAdmin );
    when( configAdmin.getConfiguration( "pentaho.geo.roles" ) ).thenThrow( new IOException() );

    geoContextProvider.setBundleContext( bundleContext );

    assertEquals( 0, geoContextProvider.props.size() );

    verify( configAdmin ).getConfiguration( "pentaho.geo.roles" );

  }

}