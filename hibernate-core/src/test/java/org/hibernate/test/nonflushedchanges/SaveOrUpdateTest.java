/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.nonflushedchanges;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.bytecode.instrumentation.internal.FieldInterceptionHelper;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.criterion.Projections;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * adapted this from "ops" tests version
 *
 * @author Gail Badner
 * @author Gavin King
 */
@FailureExpectedWithNewMetamodel
public class SaveOrUpdateTest extends AbstractOperationTestCase {
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
		cfg.setProperty( Environment.STATEMENT_BATCH_SIZE, "0" );
	}

	public String[] getMappings() {
		return new String[] { "nonflushedchanges/Node.hbm.xml" };
	}

	@Test
	@SuppressWarnings( {"UnusedAssignment"})
	public void testSaveOrUpdateDeepTree() throws Exception {
		clearCounts();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s = openSession();
		Node root = new Node( "root" );
		Node child = new Node( "child" );
		Node grandchild = new Node( "grandchild" );
		root.addChild( child );
		child.addChild( grandchild );
		s.saveOrUpdate( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( Node ) getOldToNewEntityRefMap().get( root );
		child = ( Node ) getOldToNewEntityRefMap().get( child );
		grandchild = ( Node ) getOldToNewEntityRefMap().get( grandchild );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		grandchild.setDescription( "the grand child" );
		Node grandchild2 = new Node( "grandchild2" );
		child.addChild( grandchild2 );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		s.saveOrUpdate( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( Node ) getOldToNewEntityRefMap().get( root );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertInsertCount( 1 );
		assertUpdateCount( 1 );
		clearCounts();

		Node child2 = new Node( "child2" );
		Node grandchild3 = new Node( "grandchild3" );
		child2.addChild( grandchild3 );
		root.addChild( child2 );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		s.saveOrUpdate( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		s.delete( grandchild );
		s.delete( grandchild2 );
		s.delete( grandchild3 );
		s.delete( child );
		s.delete( child2 );
		s.delete( root );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
	}

	@Test
	@SuppressWarnings( {"UnusedAssignment"})
	public void testSaveOrUpdateDeepTreeWithGeneratedId() throws Exception {
		boolean instrumented = FieldInterceptionHelper.isInstrumented( new NumberedNode() );
		clearCounts();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s = openSession();
		NumberedNode root = new NumberedNode( "root" );
		NumberedNode child = new NumberedNode( "child" );
		NumberedNode grandchild = new NumberedNode( "grandchild" );
		root.addChild( child );
		child.addChild( grandchild );
		s.saveOrUpdate( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( NumberedNode ) getOldToNewEntityRefMap().get( root );
		child = ( NumberedNode ) getOldToNewEntityRefMap().get( child );
		grandchild = ( NumberedNode ) getOldToNewEntityRefMap().get( grandchild );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		child = ( NumberedNode ) root.getChildren().iterator().next();
		grandchild = ( NumberedNode ) child.getChildren().iterator().next();
		grandchild.setDescription( "the grand child" );
		NumberedNode grandchild2 = new NumberedNode( "grandchild2" );
		child.addChild( grandchild2 );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		s.saveOrUpdate( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( NumberedNode ) getOldToNewEntityRefMap().get( root );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertInsertCount( 1 );
		assertUpdateCount( instrumented ? 1 : 3 );
		clearCounts();

		NumberedNode child2 = new NumberedNode( "child2" );
		NumberedNode grandchild3 = new NumberedNode( "grandchild3" );
		child2.addChild( grandchild3 );
		root.addChild( child2 );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		s.saveOrUpdate( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertInsertCount( 2 );
		assertUpdateCount( instrumented ? 0 : 4 );
		clearCounts();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		s.createQuery( "delete from NumberedNode where name like 'grand%'" ).executeUpdate();
		s.createQuery( "delete from NumberedNode where name like 'child%'" ).executeUpdate();
		s.createQuery( "delete from NumberedNode" ).executeUpdate();
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
	}

	@Test
	@SuppressWarnings( {"UnusedAssignment"})
	public void testSaveOrUpdateTree() throws Exception {
		clearCounts();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s = openSession();
		Node root = new Node( "root" );
		Node child = new Node( "child" );
		root.addChild( child );
		s.saveOrUpdate( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( Node ) getOldToNewEntityRefMap().get( root );
		child = ( Node ) getOldToNewEntityRefMap().get( child );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertInsertCount( 2 );
		clearCounts();

		root.setDescription( "The root node" );
		child.setDescription( "The child node" );

		Node secondChild = new Node( "second child" );

		root.addChild( secondChild );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		s.saveOrUpdate( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertInsertCount( 1 );
		assertUpdateCount( 2 );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		s.createQuery( "delete from Node where parent is not null" ).executeUpdate();
		s.createQuery( "delete from Node" ).executeUpdate();
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
	}

	@Test
	@SuppressWarnings( {"UnusedAssignment"})
	public void testSaveOrUpdateTreeWithGeneratedId() throws Exception {
		clearCounts();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s = openSession();
		NumberedNode root = new NumberedNode( "root" );
		NumberedNode child = new NumberedNode( "child" );
		root.addChild( child );
		s.saveOrUpdate( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( NumberedNode ) getOldToNewEntityRefMap().get( root );
		child = ( NumberedNode ) getOldToNewEntityRefMap().get( child );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertInsertCount( 2 );
		clearCounts();

		root.setDescription( "The root node" );
		child.setDescription( "The child node" );

		NumberedNode secondChild = new NumberedNode( "second child" );

		root.addChild( secondChild );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		s.saveOrUpdate( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertInsertCount( 1 );
		assertUpdateCount( 2 );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		s.createQuery( "delete from NumberedNode where parent is not null" ).executeUpdate();
		s.createQuery( "delete from NumberedNode" ).executeUpdate();
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
	}

	@Test
	@SuppressWarnings( {"UnusedAssignment", "UnnecessaryBoxing"})
	public void testSaveOrUpdateManaged() throws Exception {
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s = openSession();
		NumberedNode root = new NumberedNode( "root" );
		s.saveOrUpdate( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( NumberedNode ) getOldToNewEntityRefMap().get( root );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		root = ( NumberedNode ) s.get( NumberedNode.class, root.getId() );
		NumberedNode child = new NumberedNode( "child" );
		root.addChild( child );
		s.saveOrUpdate( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( NumberedNode ) getOldToNewEntityRefMap().get( root );
		assertNull( getOldToNewEntityRefMap().get( child ) );
		s.flush();
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( NumberedNode ) getOldToNewEntityRefMap().get( root );
		child = ( NumberedNode ) getOldToNewEntityRefMap().get( child );
		child = ( NumberedNode ) root.getChildren().iterator().next();
		assertTrue( s.contains( child ) );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( NumberedNode ) getOldToNewEntityRefMap().get( root );
		child = ( NumberedNode ) getOldToNewEntityRefMap().get( child );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertTrue( root.getChildren().contains( child ) );
		assertEquals( root.getChildren().size(), 1 );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		assertEquals(
				Long.valueOf( 2 ),
				s.createCriteria( NumberedNode.class )
						.setProjection( Projections.rowCount() )
						.uniqueResult()
		);
		s.delete( root );
		s.delete( child );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
	}

	@Test
	@SuppressWarnings( {"UnusedAssignment", "UnnecessaryBoxing"})
	public void testSaveOrUpdateGot() throws Exception {
		boolean instrumented = FieldInterceptionHelper.isInstrumented( new NumberedNode() );

		clearCounts();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s = openSession();
		NumberedNode root = new NumberedNode( "root" );
		s.saveOrUpdate( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( NumberedNode ) getOldToNewEntityRefMap().get( root );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertInsertCount( 1 );
		assertUpdateCount( 0 );
		clearCounts();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		s.saveOrUpdate( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( NumberedNode ) getOldToNewEntityRefMap().get( root );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertInsertCount( 0 );
		assertUpdateCount( instrumented ? 0 : 1 );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		root = ( NumberedNode ) s.get( NumberedNode.class, Long.valueOf( root.getId() ) );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( NumberedNode ) getOldToNewEntityRefMap().get( root );
		Hibernate.initialize( root.getChildren() );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( NumberedNode ) getOldToNewEntityRefMap().get( root );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		clearCounts();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		NumberedNode child = new NumberedNode( "child" );
		root.addChild( child );
		s.saveOrUpdate( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( NumberedNode ) getOldToNewEntityRefMap().get( root );
		assertTrue( Hibernate.isInitialized( root.getChildren() ) );
		child = ( NumberedNode ) root.getChildren().iterator().next();
		assertTrue( s.contains( child ) );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertInsertCount( 1 );
		assertUpdateCount( instrumented ? 0 : 1 );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		assertEquals(
				s.createCriteria( NumberedNode.class )
						.setProjection( Projections.rowCount() )
						.uniqueResult(),
				new Long( 2 )
		);
		s.delete( root );
		s.delete( child );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
	}

	@Test
	@SuppressWarnings( {"UnusedAssignment", "UnnecessaryBoxing"})
	public void testSaveOrUpdateGotWithMutableProp() throws Exception {
		clearCounts();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s = openSession();
		Node root = new Node( "root" );
		s.saveOrUpdate( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( Node ) getOldToNewEntityRefMap().get( root );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertInsertCount( 1 );
		assertUpdateCount( 0 );
		clearCounts();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		s.saveOrUpdate( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( Node ) getOldToNewEntityRefMap().get( root );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertInsertCount( 0 );
		assertUpdateCount( 0 );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		root = ( Node ) s.get( Node.class, "root" );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( Node ) getOldToNewEntityRefMap().get( root );
		Hibernate.initialize( root.getChildren() );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( Node ) getOldToNewEntityRefMap().get( root );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		clearCounts();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		Node child = new Node( "child" );
		root.addChild( child );
		s.saveOrUpdate( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( Node ) getOldToNewEntityRefMap().get( root );
		child = ( Node ) root.getChildren().iterator().next();
		assertTrue( s.contains( child ) );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( Node ) getOldToNewEntityRefMap().get( root );
		child = ( Node ) getOldToNewEntityRefMap().get( child );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertInsertCount( 1 );
		//assertUpdateCount( 1 ); //note: will fail here if no second-level cache

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		assertEquals(
				Long.valueOf( 2 ),
				s.createCriteria( Node.class )
						.setProjection( Projections.rowCount() )
						.uniqueResult()
		);
		s.delete( root );
		s.delete( child );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
	}

	@Test
	@SuppressWarnings( {"UnusedAssignment"})
	public void testEvictThenSaveOrUpdate() throws Exception {
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s = openSession();
		Node parent = new Node( "1:parent" );
		Node child = new Node( "2:child" );
		Node grandchild = new Node( "3:grandchild" );
		parent.addChild( child );
		child.addChild( grandchild );
		s.saveOrUpdate( parent );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s1 = openSession();
		child = ( Node ) s1.load( Node.class, "2:child" );
		s1 = applyNonFlushedChangesToNewSessionCloseOldSession( s1 );
		child = ( Node ) getOldToNewEntityRefMap().get( child );
		assertTrue( s1.contains( child ) );
		assertFalse( Hibernate.isInitialized( child ) );
		assertTrue( s1.contains( child.getParent() ) );
		assertTrue( Hibernate.isInitialized( child ) );
		assertFalse( Hibernate.isInitialized( child.getChildren() ) );
		assertFalse( Hibernate.isInitialized( child.getParent() ) );
		assertTrue( s1.contains( child ) );
		s1 = applyNonFlushedChangesToNewSessionCloseOldSession( s1 );
		// child is an initialized proxy; after serialization, it is
		// the proxy is replaced by its implementation
		// TODO: find out if this is how this should work...
		child = ( Node ) getOldToNewEntityRefMap().get(
				( ( HibernateProxy ) child ).getHibernateLazyInitializer().getImplementation()
		);
		s1.evict( child );
		assertFalse( s1.contains( child ) );
		assertTrue( s1.contains( child.getParent() ) );

		javax.transaction.Transaction tx1 = TestingJtaPlatformImpl.INSTANCE.getTransactionManager().suspend();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s2 = openSession();
		try {
			s2.getTransaction().begin();
			s2.saveOrUpdate( child );
			fail();
		}
		catch ( HibernateException ex ) {
			// expected because parent is connected to s1
		}
		finally {
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().rollback();
		}

		s1.evict( child.getParent() );
		assertFalse( s1.contains( child.getParent() ) );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s2 = openSession();
 		s2.saveOrUpdate( child );
		s2 = applyNonFlushedChangesToNewSessionCloseOldSession( s2 );
		child = ( Node ) getOldToNewEntityRefMap().get( child );
		assertTrue( s2.contains( child ) );
		assertFalse( s1.contains( child ) );
		assertTrue( s2.contains( child.getParent() ) );
		assertFalse( s1.contains( child.getParent() ) );
		assertFalse( Hibernate.isInitialized( child.getChildren() ) );
		assertFalse( Hibernate.isInitialized( child.getParent() ) );
		assertEquals( 1, child.getChildren().size() );
		assertEquals( "1:parent", child.getParent().getName() );
		assertTrue( Hibernate.isInitialized( child.getChildren() ) );
		assertFalse( Hibernate.isInitialized( child.getParent() ) );
		assertNull( child.getParent().getDescription() );
		assertTrue( Hibernate.isInitialized( child.getParent() ) );
		s1 = applyNonFlushedChangesToNewSessionCloseOldSession( s1 );
		s2 = applyNonFlushedChangesToNewSessionCloseOldSession( s2 );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().resume( tx1 );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
//		tx1.commit();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		s.delete( s.get( Node.class, "3:grandchild" ) );
		s.delete( s.get( Node.class, "2:child" ) );
		s.delete( s.get( Node.class, "1:parent" ) );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
	}

}

