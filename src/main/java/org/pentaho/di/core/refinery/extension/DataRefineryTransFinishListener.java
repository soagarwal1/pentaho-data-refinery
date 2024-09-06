/*! ******************************************************************************
 *
 * Pentaho Community Edition
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2028-08-13
 ******************************************************************************/

package org.pentaho.di.core.refinery.extension;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.extension.ExtensionPoint;
import org.pentaho.di.core.extension.ExtensionPointInterface;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.refinery.DataProviderHelper;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.job.Job;
import org.pentaho.di.job.entries.build.JobEntryBuildModel;
import org.pentaho.di.job.entry.JobEntryCopy;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.step.BaseStepData.StepExecutionStatus;
import org.pentaho.di.trans.step.StepMetaDataCombi;
import org.pentaho.di.trans.steps.annotation.BaseAnnotationMeta;
import org.pentaho.di.trans.steps.annotation.ModelAnnotationData;
import org.pentaho.di.trans.steps.annotation.ModelAnnotationMeta;
import org.pentaho.metastore.api.IMetaStore;
import org.pentaho.metastore.api.exceptions.MetaStoreException;

@ExtensionPoint( id = "DataRefineryTransFinishListener", description = "Updates data provider info on annotated steps.",
    extensionPointId = "TransformationFinish" )

public class DataRefineryTransFinishListener implements ExtensionPointInterface {

  private static final Class<?> PKG = ModelAnnotationMeta.class;

  @Override
  public void callExtensionPoint( LogChannelInterface log, Object object ) throws KettleException {
    Trans trans = (Trans) object;
    try {
      if ( trans.getParentJob() != null ) {
        setBuildModelOutputStep( trans.getParentJob(), trans );
      }
    } catch ( KettleException e ) {
      log.logError( e.getLocalizedMessage() );
    } catch ( Exception e ) {
      log.logError( "Error setting output step for Build Model", e );
    }
    try {
      IMetaStore metaStore = trans.getMetaStore();
      updateDataProviders( log, trans, metaStore );
    } catch ( KettleException e ) {
      log.logError( e.getLocalizedMessage() );
    } catch ( Exception e ) {
      log.logError( "Error processing data providers for annotations.", e );
    }
  }

  /**
   * Sets magic variables required for build model to use output steps
   */
  public void setBuildModelOutputStep( Job job, Trans trans ) throws KettleException {
    for ( JobEntryCopy jeCopy : job.getJobMeta().getJobCopies() ) {
      if ( JobEntryBuildModel.PLUGIN_ID.equals( jeCopy.getEntry().getPluginId() ) ) {
        JobEntryBuildModel jeBuildModel = (JobEntryBuildModel) jeCopy.getEntry();
        final String outputStepName =
            StringUtils.trimToNull( job.environmentSubstitute( jeBuildModel.getOutputStep() ) );
        if ( outputStepName == null ) {
          continue;
        }
        for ( StepMetaDataCombi stepMetaData : trans.getSteps() ) {
          String stepName = StringUtils.trimToNull( stepMetaData.stepname );
          if ( outputStepName.equals( stepName ) ) {
            Map<String, Object> map = job.getExtensionDataMap();
            String key = JobEntryBuildModel.KEY_OUTPUT_STEP_PREFIX + jeBuildModel.getName();
            if ( map.containsKey( key ) ) {
              throw new KettleException(
                  "Unable to auto-model because more than one step with the same name was found: "
                      + stepMetaData.stepname );
            }
            map.put( key, stepMetaData );
          }
        }
      }
    }
  }

  /**
   * Updates shared annotation groups with data providers
   */
  public void updateDataProviders( LogChannelInterface log, Trans trans, IMetaStore metaStore )
    throws KettleException, MetaStoreException {
    log.logDebug( "searching for annotations" );
    boolean hasAnnotations = false;

    for ( StepMetaDataCombi combi : trans.getSteps() ) {
      if ( combi.meta instanceof BaseAnnotationMeta ) {
        hasAnnotations = true;
        log.logDebug( "found annotations step '" + combi.stepname + "'" );
        ModelAnnotationData maData = (ModelAnnotationData) combi.data;
        if ( maData.annotations != null && maData.annotations.isSharedDimension() ) {
          if ( metaStore == null ) {
            log.logError( BaseMessages.getString( PKG, "ModelAnnotation.Runtime.NoMetastore" ) );
            return;
          }
          log.logDebug( "found shared dimension " + maData.annotations.getName() );
          StepMetaDataCombi outCombi = getOutputStep( combi, trans );
          // TransformationFinish is called before last step is marked as not running,
          // so that step will never report STATUS_FINISHED; using status of data instead
          if ( outCombi.data.getStatus() == StepExecutionStatus.STATUS_DISPOSED ) {
            DataProviderHelper dataProviderHelper = getDataProviderHelper( metaStore );
            dataProviderHelper.updateDataProvider( maData.annotations, outCombi );
          } else {
            log.logError( BaseMessages.getString( PKG, "ModelAnnotation.Runtime.OutputStepFail",
                outCombi.stepname,
                outCombi.data.getStatus() ) );
          }
        }
      }
    }
    if ( !hasAnnotations ) {
      log.logDebug( "no annotations found" );
    }
  }

  protected DataProviderHelper getDataProviderHelper( IMetaStore mstore ) {
    return new DataProviderHelper( mstore );
  }

  private StepMetaDataCombi getOutputStep( StepMetaDataCombi annotationCombi, final Trans trans )
    throws KettleException {
    final String outStepName = getOutputStepName( annotationCombi, trans );
    for ( StepMetaDataCombi outCombi : annotationCombi.step.getTrans().getSteps() ) {
      if ( outCombi.stepname.equals( outStepName ) ) {
        return outCombi;
      }
    }
    throw new KettleException(
        BaseMessages.getString( JobEntryBuildModel.class, "BuildModelJob.Error.UnableToFindStep", outStepName ) );
  }

  private String getOutputStepName( StepMetaDataCombi annotationCombi, final Trans trans ) throws KettleException {
    if ( annotationCombi.meta instanceof BaseAnnotationMeta ) {
      String outputStepName =
          trans.environmentSubstitute( ( (BaseAnnotationMeta) annotationCombi.meta ).getTargetOutputStep() );
      if ( StringUtils.isNotBlank( outputStepName ) ) {
        return outputStepName;
      }
    }
    throw new KettleException( "output step not set" );
  }
}

