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

package org.pentaho.di.trans.steps.annotation;

import org.pentaho.agilebi.modeler.models.annotations.CreateAttribute;
import org.pentaho.agilebi.modeler.models.annotations.CreateDimensionKey;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.injection.Injection;
import org.pentaho.di.core.injection.InjectionDeep;
import org.pentaho.di.core.injection.InjectionSupported;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;

import java.util.ArrayList;
import java.util.List;

@InjectionSupported( localizationPrefix = "AnnotateStream.Injection.",
  groups = { "DIMENSION_KEY", "ATTRIBUTE" } )
@Step( id = "CreateSharedDimensions", image = "SharedDimensions.svg",
    i18nPackageName = "org.pentaho.di.trans.steps.annotation", name = "SharedDimension.TransName",
    description = "SharedDimension.TransDescription",
    documentationUrl = "Data/Streamlined_Data_Refinery/0B0/020/0C0",
    categoryDescription = "i18n:org.pentaho.di.trans.step:BaseStep.Category.Flow" )
public class SharedDimensionMeta extends BaseAnnotationMeta {

  /////////////////////////////////////////////////////
  // Temp fields required to support metadata injection
  // These will be injected via the annotation-based injection system.
  // It's up to the init() method of the SharedDimensionStep to take them and fill in the "real" fields they correspond to

  @Injection( name = "SHARED_DIMENSION_NAME" )
  protected String sharedDimensionName;

  @Injection( name = "DATA_PROVIDER_STEP" )
  protected String dataProviderStep;

  // our super class already supports CreateAttribute, no need to provide an injection point here
  @InjectionDeep
  protected transient List<CreateAttribute> createAttributeAnnotations = new ArrayList<>();

  @InjectionDeep
  protected transient List<CreateDimensionKey> createDimensionKeyAnnotations = new ArrayList<>();

  // end temp fields
  /////////////////////////////////////////////////////

  @Override
  public StepInterface getStep( StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr,
                                TransMeta transMeta, Trans trans ) {
    return new SharedDimensionStep( stepMeta, stepDataInterface, copyNr, transMeta, trans );
  }

  private static Class<?> PKG = SharedDimensionMeta.class; // for i18n purposes, needed by Translator2!!

}
