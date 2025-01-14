package com.buschmais.jqassistant.core.analysis.impl;

import com.buschmais.jqassistant.core.rule.api.executor.RuleVisitor;
import com.buschmais.jqassistant.core.rule.api.model.Concept;
import com.buschmais.jqassistant.core.rule.api.model.Constraint;
import com.buschmais.jqassistant.core.rule.api.model.Group;
import com.buschmais.jqassistant.core.rule.api.model.RuleException;
import com.buschmais.jqassistant.core.store.api.Store;
import com.buschmais.xo.api.XOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static com.buschmais.jqassistant.core.rule.api.model.Severity.MAJOR;
import static com.buschmais.jqassistant.core.rule.api.model.Severity.MINOR;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

/**
 * Verifies the functionality of the TransactionalRuleVisitor.
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class TransactionalVisitorTest {

    @Mock
    private RuleVisitor delegate;

    @Mock
    private Store store;

    private boolean activeTransaction;

    private TransactionalRuleVisitor visitor;

    @BeforeEach
    void setUp() {
        visitor = new TransactionalRuleVisitor(delegate, store);
        doAnswer(invocation -> activeTransaction = true).when(store).beginTransaction();
        doAnswer(invocation -> activeTransaction = false).when(store).commitTransaction();
        doAnswer(invocation -> activeTransaction = false).when(store).rollbackTransaction();
        doAnswer(invocation -> activeTransaction).when(store).hasActiveTransaction();
    }

    @Test
    void beforeRules() throws RuleException {
        visitor.beforeRules();

        verify(delegate).beforeRules();
        verifySuccessfulTransaction();
    }

    @Test
    void afterRules() throws RuleException {
        visitor.afterRules();

        verify(delegate).afterRules();
        verifySuccessfulTransaction();
    }

    @Test
    void beforeGroup() throws RuleException {
        Group group = mock(Group.class);

        visitor.beforeGroup(group, MAJOR);

        verify(delegate).beforeGroup(group, MAJOR);
        verifySuccessfulTransaction();
    }

    @Test
    void afterGroup() throws RuleException {
        Group group = mock(Group.class);

        visitor.afterGroup(group);

        verify(delegate).afterGroup(group);
        verifySuccessfulTransaction();
    }

    @Test
    void visitConcept() throws RuleException {
        Concept concept = mock(Concept.class);

        visitor.visitConcept(concept, MINOR);

        verify(delegate).visitConcept(concept, MINOR);
        verifySuccessfulTransaction();
    }

    @Test
    void skipConcept() throws RuleException {
        Concept concept = mock(Concept.class);

        visitor.skipConcept(concept, MINOR);

        verify(delegate).skipConcept(concept, MINOR);
        verifySuccessfulTransaction();
    }

    @Test
    void visitConstraint() throws RuleException {
        Constraint constraint = mock(Constraint.class);

        visitor.visitConstraint(constraint, MINOR);

        verify(delegate).visitConstraint(constraint, MINOR);
        verifySuccessfulTransaction();
    }

    @Test
    void skipConstraint() throws RuleException {
        Constraint constraint = mock(Constraint.class);

        visitor.skipConstraint(constraint, MAJOR);

        verify(delegate).skipConstraint(constraint, MAJOR);
        verifySuccessfulTransaction();
    }

    @Test
    void ruleException() throws RuleException {
        Constraint constraint = mock(Constraint.class);
        doThrow(new RuleException("Test")).when(delegate).visitConstraint(constraint, MAJOR);

        try {
            visitor.visitConstraint(constraint, MAJOR);
            fail("Expecting a " + RuleException.class);
        } catch (RuleException e) {
            verify(delegate).visitConstraint(constraint, MAJOR);
            verifyFailedTransaction();
        }
    }

    @Test
    void runtimeException() throws RuleException {
        Constraint constraint = mock(Constraint.class);
        doThrow(new NullPointerException()).when(delegate).visitConstraint(constraint, MAJOR);

        try {
            visitor.visitConstraint(constraint, MAJOR);
            fail("Expecting a " + XOException.class);
        } catch (RuleException e) {
            verify(delegate).visitConstraint(constraint, MAJOR);
            verifyFailedTransaction();
        }
    }

    private void verifySuccessfulTransaction() {
        verify(store).beginTransaction();
        verify(store).commitTransaction();
        verify(store, never()).rollbackTransaction();
    }

    private void verifyFailedTransaction() {
        verify(store).beginTransaction();
        verify(store).rollbackTransaction();
        verify(store, never()).commitTransaction();
    }
}
