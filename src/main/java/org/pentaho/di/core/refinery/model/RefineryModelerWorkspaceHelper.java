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

/*!
* This program is free software; you can redistribute it and/or modify it under the
* terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
* Foundation.
*
* You should have received a copy of the GNU Lesser General Public License along with this
* program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
* or from the Free Software Foundation, Inc.,
* 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*
* This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
* without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
* See the GNU Lesser General Public License for more details.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package org.pentaho.di.core.refinery.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.swt.widgets.Display;
import org.pentaho.agilebi.modeler.BaseModelerWorkspaceHelper;
import org.pentaho.agilebi.modeler.IModelerWorkspaceHelper;
import org.pentaho.agilebi.modeler.ModelerException;
import org.pentaho.agilebi.modeler.ModelerWorkspace;
import org.pentaho.agilebi.modeler.geo.GeoContext;
import org.pentaho.agilebi.modeler.nodes.AvailableField;
import org.pentaho.agilebi.modeler.nodes.MainModelNode;
import org.pentaho.agilebi.modeler.nodes.RelationalModelNode;
import org.pentaho.agilebi.modeler.strategy.AutoModelStrategy;
import org.pentaho.agilebi.modeler.strategy.SimpleAutoModelStrategy;
import org.pentaho.agilebi.modeler.util.ModelerWorkspaceHelper;
import org.pentaho.metadata.model.concept.types.LocalizedString;

/**
 * Helper for auto-modeling from the data refinery Build Model step
 */
public class RefineryModelerWorkspaceHelper extends BaseModelerWorkspaceHelper implements IModelerWorkspaceHelper {

  final ModelerWorkspaceHelper helper;

  final GeoContext geoContext;

  public RefineryModelerWorkspaceHelper( GeoContext geoContext ) {
    super( LocalizedString.DEFAULT_LOCALE );
    helper = new ModelerWorkspaceHelper( LocalizedString.DEFAULT_LOCALE );
    this.geoContext = geoContext;
    helper.setAutoModelStrategy( new SimpleAutoModelStrategy( LocalizedString.DEFAULT_LOCALE, this.geoContext ) );
  }

  /**
   * Builds an OLAP model that is attribute based.
   *
   * @param workspace
   */
  public void autoModelFlat( ModelerWorkspace workspace ) throws ModelerException {
    workspace.setGeoContext( this.geoContext );
    final AutoModelStrategy strategy = getAutoModelStrategy();
    strategy.autoModelOlap( workspace, workspace.getModel() );
    strategy.autoModelRelational( workspace, workspace.getRelationalModel() );
  }


  /**
   * Builds an OLAP model that is attribute based.
   *
   * @param workspace
   */
  public void autoModelFlatInBackground( ModelerWorkspace workspace ) throws ModelerException {
    workspace.setGeoContext( this.geoContext );
    final ModelerWorkspace ws = workspace;
    final AutoModelStrategy strategy = getAutoModelStrategy();

    final Display display = Display.findDisplay( Thread.currentThread() );
    Runnable worker = new Runnable() {

      public void run() {
        final boolean prevChangeState = ws.isModelChanging();
        try {

          MainModelNode node = ws.getModel();
          node.setSupressEvents( true );
          strategy.autoModelOlap( ws, node );
        } catch ( ModelerException e ) {
          // ignore it
        }
        display.syncExec( new Runnable() {

          public void run() {
            ws.getModel().setSupressEvents( false );
            ws.setModelIsChanging( prevChangeState, true );
            ws.setSelectedNode( ws.getModel() );
          }
        } );
      }
    };
    new Thread( worker ).start();


  }

  /**
   * Builds a Relational Model that is attribute based, all available fields are added into a single Category
   *
   * @param workspace
   * @throws ModelerException
   */
  public void autoModelRelationalFlat( ModelerWorkspace workspace ) throws ModelerException {
    helper.autoModelRelationalFlat( workspace );
  }

  /**
   * Builds a Relational Model that is attribute based, all available fields are added into a single Category
   *
   * @param workspace
   * @throws ModelerException
   */
  public void autoModelRelationalFlatInBackground( final ModelerWorkspace workspace ) throws ModelerException {

    final AutoModelStrategy strategy = getAutoModelStrategy();

    final Display display = Display.findDisplay( Thread.currentThread() );
    Runnable worker = new Runnable() {

      public void run() {
        final boolean prevChangeState = workspace.isModelChanging();
        try {
          RelationalModelNode node = workspace.getRelationalModel();
          node.setSupressEvents( true );
          strategy.autoModelRelational( workspace, node );
        } catch ( ModelerException e ) {
          // ignore it
        }
        display.syncExec( new Runnable() {

          public void run() {
            workspace.getRelationalModel().setSupressEvents( false );
            workspace.setRelationalModelIsChanging( prevChangeState, true );
            workspace.setSelectedRelationalNode( workspace.getRelationalModel() );
          }
        } );
      }
    };
    new Thread( worker ).start();
  }

  @Override
  protected MainModelNode getMainModelNode( ModelerWorkspace workspace ) {
    return new MainModelNode();
  }

  @Override
  protected RelationalModelNode getRelationalModelNode( ModelerWorkspace workspace ) {
    return new RelationalModelNode();
  }

  public void sortFields( List<AvailableField> availableFields ) {
    Collections.sort( availableFields, new Comparator<AvailableField>() {
      public int compare( AvailableField o1, AvailableField o2 ) {
        if ( o1 == null && o2 == null ) {
          return 0;
        } else if ( o1 == null ) {
          return -1;
        } else if ( o2 == null ) {
          return 1;
        }
        String name1 = o1.getDisplayName();
        String name2 = o2.getDisplayName();
        if ( name1 == null && name2 == null ) {
          return 0;
        } else if ( name1 == null ) {
          return -1;
        } else if ( name2 == null ) {
          return 1;
        }
        return name1.compareToIgnoreCase( name2 );
      }
    } );
  }

  public AutoModelStrategy getAutoModelStrategy() {
    AutoModelStrategy strategy = super.getAutoModelStrategy();

    if ( strategy instanceof SimpleAutoModelStrategy ) {
      ( (SimpleAutoModelStrategy) strategy ).setGeoContext( geoContext );
    }
    return strategy;
  }
}
