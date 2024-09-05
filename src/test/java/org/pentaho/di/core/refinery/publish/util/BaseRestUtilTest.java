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

package org.pentaho.di.core.refinery.publish.util;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import org.junit.Assert;
import org.junit.Test;
import org.pentaho.di.core.KettleClientEnvironment;
import org.pentaho.di.core.util.EnvUtil;

import static org.junit.Assert.*;


public class BaseRestUtilTest {

  @Test
  public void getAnonymousClient() throws Exception {

    //without property default value 2000
    Client anonymousClient = new PublishRestUtil().getAnonymousClient();
    Assert.assertEquals( 2000, anonymousClient.getProperties().get( ClientConfig.PROPERTY_READ_TIMEOUT ) );

    int timeOut = 5000;
    System.setProperty( BaseRestUtil.KETTLE_DATA_REFINERY_HTTP_CLIENT_TIMEOUT, String.valueOf( timeOut ) );
    anonymousClient = new PublishRestUtil().getAnonymousClient();
    Assert.assertEquals( timeOut, anonymousClient.getProperties().get( ClientConfig.PROPERTY_READ_TIMEOUT ) );
  }

}
