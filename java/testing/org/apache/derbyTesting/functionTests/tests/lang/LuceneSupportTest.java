/**
 *  Derby - Class org.apache.derbyTesting.functionTests.tests.lang.LuceneSupportTest
 *  
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.derbyTesting.functionTests.tests.lang;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Statement;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.derby.shared.common.reference.SQLState;
import org.apache.derbyTesting.junit.BaseJDBCTestCase;
import org.apache.derbyTesting.junit.JDBC;
import org.apache.derbyTesting.junit.SecurityManagerSetup;
import org.apache.derbyTesting.junit.TestConfiguration;

/**
 * <p>
 * Basic test of the optional tool which provides Lucene indexing of
 * columns in Derby tables.
 * </p>
 */
public class LuceneSupportTest extends BaseJDBCTestCase {

    private static  final   String  ILLEGAL_CHARACTER = "42XBD";
    
	public LuceneSupportTest(String name) {
		super(name);
	}
	
	public static Test suite() {
		TestSuite suite = new TestSuite("LuceneSupportTest");
		
		suite.addTest(SecurityManagerSetup.noSecurityManager(TestConfiguration.embeddedSuite(LuceneSupportTest.class)));
 
		return suite;
	}
	
	public void testCreateAndQueryIndex() throws Exception {
		CallableStatement cSt;
		Statement s = createStatement();
	    
		cSt = prepareCall
            ( "call LuceneSupport.createIndex('lucenetest','titles','title', null )" );
	    assertUpdateCount(cSt, 0);
	    
	    String[][] expectedRows = new String[][]
            {
                { "1","0","0.8048013" },
	    		{ "3","2","0.643841" }
            };
	    JDBC.assertFullResultSet
            (
             s.executeQuery
             (
              "select * from table ( lucenetest.titles__title( 'grapes', null, 1000, 0 ) ) luceneResults"
              ),
             expectedRows
             );

	    expectedRows = new String[][]
            {
	    		{ "3","2","0.643841" }
            };
	    JDBC.assertFullResultSet
            (
             s.executeQuery
             (
              "select * from table ( lucenetest.titles__title( 'grapes', null, 1000, .75 ) ) luceneResults"
              ),
             expectedRows
             );

	    JDBC.assertEmpty
            (
             s.executeQuery
             (
              "select * from table ( lucenetest.titles__title( 'grapes',  null, 1000, 0.5) ) luceneResults"
              )
             );

	    expectedRows = new String[][]
            {
                { "The Grapes Of Wrath", "John Steinbeck", "The Viking Press", "0"},
	    		{"Vines, Grapes, and Wines", "Jancis Robinson", "Alfred A. Knopf", "2"}
            };
	    JDBC.assertFullResultSet
            (
             s.executeQuery
             (
              "select title, author, publisher, documentID\n" +
              "from lucenetest.titles t, table ( lucenetest.titles__title( 'grapes', null, 1000, 0 ) ) l\n" +
              "where t.id = l.id\n" 
              ),
             expectedRows
             );
	   
		cSt = prepareCall
            ( "call LuceneSupport.dropIndex('lucenetest','titles','title')" );
	    assertUpdateCount(cSt, 0);

	}
	
	public void testUpdateIndex() throws Exception {
		CallableStatement cSt;
		Statement s = createStatement();
		
		cSt = prepareCall
            ( "call LuceneSupport.createIndex('lucenetest','titles','title', null)" );
	    assertUpdateCount(cSt, 0);

	    JDBC.assertEmpty
            (
             s.executeQuery
             (
              "select *\n" +
              "from table ( lucenetest.titles__title( 'mice', null, 1000, 0 ) ) luceneResults\n"
              )
             );
	    
	    cSt = prepareCall( "update TITLES SET TITLE='Of Mice and Men' WHERE ID=1" );
	    assertUpdateCount(cSt, 1);
	    
	    JDBC.assertEmpty
            (
             s.executeQuery
             (
              "select *\n" +
              "from table ( lucenetest.titles__title( 'mice', null, 1000, 0 ) ) luceneResults\n"
              )
             );
	    
		cSt = prepareCall
            ( "call LuceneSupport.updateIndex('lucenetest','titles','title', null)" );
	    assertUpdateCount(cSt, 0);

	    String[][] expectedRows = new String[][]
            {
                { "1","0","1.058217" }
            };
	    JDBC.assertFullResultSet
            (
             s.executeQuery
             (
              "select *\n" +
              "from table ( lucenetest.titles__title( 'mice', null, 1000, 0 ) ) luceneResults\n"
              ),
             expectedRows
             );

		cSt = prepareCall
            ( "call LuceneSupport.dropIndex('lucenetest','titles','title')" );
	    assertUpdateCount(cSt, 0);

	}
	
	public void testListIndex() throws Exception {
		CallableStatement cSt;
		Statement s = createStatement();

	    cSt = prepareCall
            ( "call LuceneSupport.createIndex('lucenetest','titles','title', null)" );
	    assertUpdateCount(cSt, 0);
	    
		cSt = prepareCall
            ( "call LuceneSupport.createIndex('lucenetest','titles','author', null)" );
	    assertUpdateCount(cSt, 0);
	    
	    // leave out lastmodified as the date will change
	    String[][] expectedRows = new String[][]
            {
                { "LUCENETEST", "TITLES", "AUTHOR" },
	    		{ "LUCENETEST", "TITLES", "TITLE" }
            };
	    JDBC.assertFullResultSet
            (
             s.executeQuery
             (
              "select schemaname, tablename, columnname from table ( LuceneSupport.listIndexes() ) listindexes order by schemaname, tablename, columnname"
              ),
             expectedRows
             );

		cSt = prepareCall
            ( "call LuceneSupport.dropIndex('lucenetest','titles','title')" );
	    assertUpdateCount(cSt, 0);

	    expectedRows = new String[][]
            {
                { "LUCENETEST", "TITLES", "AUTHOR" },
            };
	    JDBC.assertFullResultSet
            (
             s.executeQuery
             (
              "select schemaname, tablename, columnname from table ( LuceneSupport.listIndexes() ) listindexes order by schemaname, tablename, columnname"
              ),
             expectedRows
             );

		cSt = prepareCall
            ( "call LuceneSupport.dropIndex('lucenetest','titles','author')" );
	    assertUpdateCount(cSt, 0);
	    
	    JDBC.assertEmpty
            (
             s.executeQuery
             (
              "select schemaname, tablename, columnname from table ( LuceneSupport.listIndexes() ) listindexes"
              )
             );

	}
	
	public void testDropIndexBadCharacters() throws Exception {
		CallableStatement st;
	    
		assertCallError( ILLEGAL_CHARACTER, "call LuceneSupport.dropIndex('../','','')");
		assertCallError( ILLEGAL_CHARACTER, "call LuceneSupport.dropIndex('','../','')");
		assertCallError( ILLEGAL_CHARACTER, "call LuceneSupport.dropIndex('','','../')");
		
	}
	
	protected void setUp() throws SQLException {
		CallableStatement cSt;			    
		Statement st = createStatement();
		
		try {
			st.executeUpdate("create schema lucenetest");
		} catch (Exception e) {
		}
		st.executeUpdate("set schema lucenetest");	
		st.executeUpdate("create table titles (ID int generated always as identity primary key, ISBN varchar(16), PRINTISBN varchar(16), title varchar(1024), subtitle varchar(1024), author varchar(1024), series varchar(1024), publisher varchar(1024), collections varchar(128), collections2 varchar(128))");
		st.executeUpdate("insert into titles (ISBN, PRINTISBN, TITLE, SUBTITLE, AUTHOR, SERIES, PUBLISHER, COLLECTIONS, COLLECTIONS2) values ('9765087650324','9765087650324','The Grapes Of Wrath','The Great Depression in Oklahoma','John Steinbeck','Noble Winners','The Viking Press','National Book Award','Pulitzer Prize')");
		st.executeUpdate("insert into titles (ISBN, PRINTISBN, TITLE, SUBTITLE, AUTHOR, SERIES, PUBLISHER, COLLECTIONS, COLLECTIONS2) values ('6754278542987','6754278542987','Identical: Portraits of Twins','Best Photo Book 2012 by American Photo Magazine','Martin Schoeller','Portraits','teNeues','Photography','')");
		st.executeUpdate("insert into titles (ISBN, PRINTISBN, TITLE, SUBTITLE, AUTHOR, SERIES, PUBLISHER, COLLECTIONS, COLLECTIONS2) values ('2747583475882','2747583475882','Vines, Grapes, and Wines','The wine drinker''s guide to grape varieties','Jancis Robinson','Reference','Alfred A. Knopf','Wine','')");	
		st.executeUpdate("insert into titles (ISBN, PRINTISBN, TITLE, SUBTITLE, AUTHOR, SERIES, PUBLISHER, COLLECTIONS, COLLECTIONS2) values ('4356123483483','4356123483483','A Tale of Two Cities','A fictional account of events leading up to the French revolution','Charles Dickens','Classics','Chapman & Hall','Fiction','Social Criticism')");	

		cSt = prepareCall
            ( "call syscs_util.syscs_register_tool('luceneSupport',true)" );
	    assertUpdateCount(cSt, 0);

	}
	
	protected void tearDown() throws Exception {
		CallableStatement cSt;
		Statement st = createStatement();
		
		st.executeUpdate("drop table titles");
		
		cSt = prepareCall
            ( "call syscs_util.syscs_register_tool('luceneSupport',false)" );
	    assertUpdateCount(cSt, 0);
	    super.tearDown();
	}
}
