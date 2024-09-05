/*! ******************************************************************************
 *
 * Pentaho Community Edition Project: data-refinery-pdi-plugin
 *
 * Copyright (C) 2002-2017 by Hitachi Vantara : http://www.pentaho.com
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

package org.pentaho.di.trans.util;

import org.junit.BeforeClass;
import org.junit.Test;
import org.pentaho.di.core.KettleClientEnvironment;
import org.pentaho.di.core.Props;
import org.pentaho.di.core.ProvidesDatabaseConnectionInformation;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.plugins.PluginRegistry;
import org.pentaho.di.core.plugins.StepPluginType;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.steps.tableoutput.TableOutputMeta;
import org.pentaho.metastore.api.IMetaStore;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransUtilTest {
  @Test
  public void testCollectionsAllTableOutputSteps() throws Exception {
    final TransMeta transMeta = mock( TransMeta.class );
    final Repository repository = mock( Repository.class );
    final IMetaStore metastore = mock( IMetaStore.class );
    final StepMeta outputStep1 = mock( StepMeta.class );
    when( outputStep1.getName() ).thenReturn( "outputStep1" );
    final StepMeta outputStep2 = mock( StepMeta.class );
    when( outputStep2.getName() ).thenReturn( "outputStep2" );
    final StepMeta notOutputStep = mock( StepMeta.class );
    when( notOutputStep.getName() ).thenReturn( "notOutputStep" );
    when( transMeta.getSteps() ).thenReturn( Arrays.asList( outputStep1, outputStep2, notOutputStep ) );
    final StepMetaInterface outputInterface1 = mock( TableOutputMeta.class );
    when( outputStep1.getStepMetaInterface() ).thenReturn( outputInterface1 );
    final StepMetaInterface outputInterface2 = mock( TableOutputMeta.class );
    when( outputStep2.getStepMetaInterface() ).thenReturn( outputInterface2 );
    final StepMetaInterface notOutputInterface = mock( StepMetaInterface.class );
    when( notOutputStep.getStepMetaInterface() ).thenReturn( notOutputInterface );
    Map<String, ProvidesDatabaseConnectionInformation> stepMap =
        TransUtil.collectOutputStepInTrans( transMeta, repository, metastore );
    assertEquals( 2, stepMap.size() );
    assertNotNull( stepMap.get( "outputStep1" ) );
    assertNotNull( stepMap.get( "outputStep2" ) );
    assertNull( stepMap.get( "notOutputStep" ) );
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
  public void testResetParams() throws Exception {

    TransMeta meta = new TransMeta();
    String paramA = "paramA";
    meta.addParameterDefinition( paramA, "defA", "desc" );
    meta.setParameterValue( paramA, "other" );
    meta.setVariable( paramA, "other" );
    LogChannelInterface logChannel = mock( LogChannelInterface.class );

    TransUtil.resetParams( meta, logChannel );
    assertEquals( "defA", meta.getParameterValue( paramA ) );
    assertEquals( "defA", meta.getVariable( paramA ) );
  }
}
