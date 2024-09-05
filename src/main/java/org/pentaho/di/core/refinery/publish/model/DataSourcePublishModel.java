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

package org.pentaho.di.core.refinery.publish.model;

import java.io.Serializable;

import org.pentaho.di.core.refinery.publish.agilebi.BiServerConnection;

/**
 * @author Rowell Belen
 */
public class DataSourcePublishModel implements Serializable {

  private static final long serialVersionUID = 7797060550124837560L;
  public static final String ACCESS_TYPE_EVERYONE = "everyone";
  public static final String ACCESS_TYPE_USER = "user";
  public static final String ACCESS_TYPE_ROLE = "role";

  private String modelName = "";
  private boolean override;
  private String userOrRole;
  private String accessType = ACCESS_TYPE_EVERYONE;

  private BiServerConnection biServerConnection;

  public BiServerConnection getBiServerConnection() {
    return biServerConnection;
  }

  public void setBiServerConnection( BiServerConnection biServerConnection ) {
    this.biServerConnection = biServerConnection;
  }

  public String getModelName() {
    return modelName;
  }

  public void setModelName( String modelName ) {
    this.modelName = modelName;
  }

  public boolean isOverride() {
    return override;
  }

  public void setOverride( boolean override ) {
    this.override = override;
  }

  public String getUserOrRole() {
    return userOrRole;
  }

  public void setUserOrRole( String userOrRole ) {
    this.userOrRole = userOrRole;
  }

  public String getAccessType() {
    return accessType;
  }

  public void setAccessType( String accessType ) {
    this.accessType = accessType;
  }
}
