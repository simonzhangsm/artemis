/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.apache.ambari.server.api.predicate;

import org.apache.ambari.server.api.predicate.expressions.Expression;
import org.apache.ambari.server.api.predicate.expressions.LogicalExpressionFactory;
import org.apache.ambari.server.api.predicate.expressions.RelationalExpression;
import org.apache.ambari.server.api.predicate.operators.*;
import org.apache.ambari.server.controller.spi.Predicate;

import java.util.*;

/**
 * Parser which produces a predicate instance from an array of tokens, which is generated by the lexer.
 */
public class QueryParser {
	
	/**
	 * Map of token type to token handlers.
	 */
	private static final Map<Token.TYPE, TokenHandler> TOKEN_HANDLERS = new HashMap<Token.TYPE, TokenHandler>();
	
	/**
	 * Constructor. Register token handlers.
	 */
	public QueryParser() {
		TOKEN_HANDLERS.put(Token.TYPE.BRACKET_OPEN, new BracketOpenTokenHandler());
		TOKEN_HANDLERS.put(Token.TYPE.BRACKET_CLOSE, new BracketCloseTokenHandler());
		TOKEN_HANDLERS.put(Token.TYPE.RELATIONAL_OPERATOR, new RelationalOperatorTokenHandler());
		TOKEN_HANDLERS.put(Token.TYPE.LOGICAL_OPERATOR, new LogicalOperatorTokenHandler());
		TOKEN_HANDLERS.put(Token.TYPE.LOGICAL_UNARY_OPERATOR, new LogicalUnaryOperatorTokenHandler());
		TOKEN_HANDLERS.put(Token.TYPE.PROPERTY_OPERAND, new PropertyOperandTokenHandler());
		TOKEN_HANDLERS.put(Token.TYPE.VALUE_OPERAND, new ValueOperandTokenHandler());
		TOKEN_HANDLERS.put(Token.TYPE.RELATIONAL_OPERATOR_FUNC, new RelationalOperatorFuncTokenHandler());
	}
	
	/**
	 * Generate a Predicate instance from an array of tokens. Each input token contains a type and a value. Based on the token type and location, the tokens are first translated into a list of expressions, both relational and logical. These expressions are then merged into a tree
	 * of expressions with a single root following operator precedence and explicit grouping rules. Depending on the query, this merging of expressions into a tree of expressions may occur in several passes, one pass per level of precedence starting at the highest level of
	 * precedence. The predicate is built by traversing the expression tree in-order with each node expressing itself as a predicate.
	 * 
	 * @param tokens an array of tokens which represent the query, each token contains type and value information
	 * @return a new predicate instance based on the supplied tokens
	 * @throws InvalidQueryException if unable to parse the tokens and produce a predicate
	 */
	public Predicate parse(Token[] tokens) throws InvalidQueryException {
		ParseContext ctx = parseExpressions(tokens);
		
		List<Expression> listExpressions = ctx.getExpressions();
		List<Expression> listMergedExpressions = mergeExpressions(listExpressions, ctx.getMaxPrecedence());
		
		return listMergedExpressions.isEmpty() ? null : listMergedExpressions.get(0).toPredicate();
	}
	
	/**
	 * Create parse context from an array of tokens. The parse context contains a list of expressions and other information about the expressions an parsed tokens.
	 * 
	 * @param tokens an array of tokens which represent the query, each token contains type and value information
	 * @return a parse context which contains a list of expressions
	 * @throws InvalidQueryException if unable to properly parse the tokens into a parse context
	 */
	private ParseContext parseExpressions(Token[] tokens) throws InvalidQueryException {
		ParseContext ctx = new ParseContext(tokens);
		
		while (ctx.getCurrentTokensIndex() < tokens.length) {
			TOKEN_HANDLERS.get(tokens[ctx.getCurrentTokensIndex()].getType()).handleToken(ctx);
		}
		
		if (ctx.getPrecedenceLevel() != 0) { throw new InvalidQueryException("Invalid query string: mismatched parentheses."); }
		
		return ctx;
	}
	
	/**
	 * Merge list of expressions into a tree of logical/relational expressions. This is done recursively in several passes, one per level of precedence starting at the highest precedence level. Recursion exits when a single expression remains.
	 * 
	 * @param listExpressions list of expressions to merge
	 * @param precedenceLevel the precedence level that is to be merged
	 * @return tree of expressions with a single root expression
	 */
	private List<Expression> mergeExpressions(List<Expression> listExpressions, int precedenceLevel) {
		if (listExpressions.size() > 1) {
			Stack<Expression> stack = new Stack<Expression>();
			
			stack.push(listExpressions.remove(0));
			while (!listExpressions.isEmpty()) {
				Expression exp = stack.pop();
				Expression left = stack.empty() ? null : stack.pop();
				Expression right = listExpressions.remove(0);
				stack.addAll(exp.merge(left, right, precedenceLevel));
			}
			return mergeExpressions(new ArrayList<Expression>(stack), precedenceLevel - 1);
		}
		return listExpressions;
	}
	
	/**
	 * A parse context which contains information related to parsing the provided tokens into expressions.
	 */
	private class ParseContext {
		/**
		 * The current context precedence level. This is dictated by bracket tokens.
		 */
		private int m_precedence = 0;
		
		/**
		 * Current position in tokens array
		 */
		private int m_tokensIdx = 0;
		
		/**
		 * Tokens
		 */
		private Token[] m_tokens;
		
		/**
		 * The type of the previous token used in validation.
		 */
		private Token.TYPE m_previousTokenType = null;
		
		/**
		 * The list of expressions which are generated from the tokens.
		 */
		private List<Expression> m_listExpressions = new ArrayList<Expression>();
		
		/**
		 * Highest precedence level in expression.
		 */
		int m_maxPrecedence = 0;
		
		public ParseContext(Token[] tokens) {
			m_tokens = tokens;
		}
		
		/**
		 * Get array of all tokens.
		 * 
		 * @return token array
		 */
		public Token[] getTokens() {
			return m_tokens;
		}
		
		/**
		 * Get the current position in the tokens array.
		 * 
		 * @return the current tokens index
		 */
		public int getCurrentTokensIndex() {
			return m_tokensIdx;
		}
		
		/**
		 * Set the current position in the tokens array. Each handler should set this value after processing a token(s).
		 * 
		 * @param idx current tokens index
		 */
		public void setCurrentTokensIndex(int idx) {
			m_tokensIdx = idx;
		}
		
		/**
		 * Increment the context precedence level.
		 * 
		 * @param val how much the level is increased by
		 */
		public void incPrecedenceLevel(int val) {
			m_precedence += val;
		}
		
		/**
		 * Decrement the context precedence level.
		 * 
		 * @param val how much the level is decremented by
		 * @throws InvalidQueryException if the level is decremented below 0
		 */
		public void decPrecedenceLevel(int val) throws InvalidQueryException {
			m_precedence -= val;
			if (m_precedence < 0) { throw new InvalidQueryException("Invalid query string: mismatched parentheses."); }
		}
		
		/**
		 * Get the current context precedence level.
		 * 
		 * @return current context precedence level
		 */
		public int getPrecedenceLevel() {
			return m_precedence;
		}
		
		/**
		 * Get the list of generated expressions.
		 * 
		 * @return the list of generated expressions
		 */
		public List<Expression> getExpressions() {
			return m_listExpressions;
		}
		
		/**
		 * Get the last expression.
		 * 
		 * @return the last expression
		 */
		public Expression getPrecedingExpression() {
			return m_listExpressions == null ? null : m_listExpressions.get(m_listExpressions.size() - 1);
		}
		
		/**
		 * Get the highest operator precedence in the list of generated expressions.
		 * 
		 * @return the max operator precedence
		 */
		public int getMaxPrecedence() {
			return m_maxPrecedence;
		}
		
		/**
		 * Update the max precedence level. The max precedence level is only updated if the provided level > the current level.
		 * 
		 * @param precedenceLevel the new value
		 */
		public void updateMaxPrecedence(int precedenceLevel) {
			if (precedenceLevel > m_maxPrecedence) {
				m_maxPrecedence = precedenceLevel;
			}
		}
		
		/**
		 * Add a generated expression.
		 * 
		 * @param exp the expression to add
		 */
		public void addExpression(Expression exp) {
			m_listExpressions.add(exp);
		}
		
		/**
		 * Set the token type of the current token
		 * 
		 * @param type type of the current token
		 */
		private void setTokenType(Token.TYPE type) {
			m_previousTokenType = type;
		}
		
		/**
		 * Get the last token type set.
		 * 
		 * @return the last token type set
		 */
		public Token.TYPE getPreviousTokenType() {
			return m_previousTokenType;
		}
	}
	
	/**
	 * Base token handler. Token handlers are responsible for handling the processing of a specific token type.
	 */
	private abstract class TokenHandler {
		/**
		 * Process a token. Handles common token processing functionality then delegates to the individual concrete handlers.
		 * 
		 * @param ctx the current parse context
		 * @throws InvalidQueryException if unable to process the token
		 */
		public void handleToken(ParseContext ctx) throws InvalidQueryException {
			Token token = ctx.getTokens()[ctx.getCurrentTokensIndex()];
			if (!validate(ctx.getPreviousTokenType())) { throw new InvalidQueryException("Unexpected token encountered in query string. Last Token Type=" + ctx.getPreviousTokenType() + ", Current Token[type=" + token.getType() + ", value='" + token.getValue() + "']"); }
			ctx.setTokenType(token.getType());
			
			int idxIncrement = _handleToken(ctx);
			ctx.setCurrentTokensIndex(ctx.getCurrentTokensIndex() + idxIncrement);
		}
		
		/**
		 * Process a token.
		 * 
		 * @param ctx the current parse context
		 * @throws InvalidQueryException if unable to process the token
		 */
		public abstract int _handleToken(ParseContext ctx) throws InvalidQueryException;
		
		/**
		 * Validate the token based on the previous token.
		 * 
		 * @param previousTokenType the type of the previous token
		 * @return true if validation is successful, false otherwise
		 */
		public abstract boolean validate(Token.TYPE previousTokenType);
	}
	
	/**
	 * Open Bracket token handler.
	 */
	private class BracketOpenTokenHandler extends TokenHandler {
		
		@Override
		public int _handleToken(ParseContext ctx) {
			ctx.incPrecedenceLevel(Operator.MAX_OP_PRECEDENCE);
			return 1;
		}
		
		@Override
		public boolean validate(Token.TYPE previousTokenType) {
			return previousTokenType == null || previousTokenType == Token.TYPE.BRACKET_OPEN || previousTokenType == Token.TYPE.LOGICAL_OPERATOR || previousTokenType == Token.TYPE.LOGICAL_UNARY_OPERATOR;
		}
	}
	
	/**
	 * Close Bracket token handler
	 */
	private class BracketCloseTokenHandler extends TokenHandler {
		@Override
		public int _handleToken(ParseContext ctx) throws InvalidQueryException {
			ctx.decPrecedenceLevel(Operator.MAX_OP_PRECEDENCE);
			
			return 1;
		}
		
		@Override
		public boolean validate(Token.TYPE previousTokenType) {
			return previousTokenType == Token.TYPE.VALUE_OPERAND || previousTokenType == Token.TYPE.BRACKET_CLOSE;
		}
	}
	
	/**
	 * Relational Operator token handler
	 */
	private class RelationalOperatorTokenHandler extends TokenHandler {
		@Override
		public int _handleToken(ParseContext ctx) throws InvalidQueryException {
			Token token = ctx.getTokens()[ctx.getCurrentTokensIndex()];
			RelationalOperator relationalOp = RelationalOperatorFactory.createOperator(token.getValue());
			// todo: use factory to create expression
			ctx.addExpression(new RelationalExpression(relationalOp));
			
			return 1;
		}
		
		@Override
		public boolean validate(Token.TYPE previousTokenType) {
			return previousTokenType == null || previousTokenType == Token.TYPE.BRACKET_OPEN || previousTokenType == Token.TYPE.LOGICAL_OPERATOR || previousTokenType == Token.TYPE.LOGICAL_UNARY_OPERATOR;
		}
	}
	
	/**
	 * Relational Operator function token handler
	 */
	private class RelationalOperatorFuncTokenHandler extends TokenHandler {
		@Override
		public int _handleToken(ParseContext ctx) throws InvalidQueryException {
			Token[] tokens = ctx.getTokens();
			int idx = ctx.getCurrentTokensIndex();
			Token token = tokens[idx];
			RelationalOperator relationalOp = RelationalOperatorFactory.createOperator(token.getValue());
			
			ctx.addExpression(new RelationalExpression(relationalOp));
			ctx.setCurrentTokensIndex(++idx);
			
			TokenHandler propertyHandler = new PropertyOperandTokenHandler();
			propertyHandler.handleToken(ctx);
			
			// handle right operand if applicable to operator
			idx = ctx.getCurrentTokensIndex();
			if (ctx.getCurrentTokensIndex() < tokens.length && tokens[idx].getType().equals(Token.TYPE.VALUE_OPERAND)) {
				TokenHandler valueHandler = new ValueOperandTokenHandler();
				valueHandler.handleToken(ctx);
			}
			
			// skip closing bracket
			idx = ctx.getCurrentTokensIndex();
			if (idx >= tokens.length || tokens[idx].getType() != Token.TYPE.BRACKET_CLOSE) { throw new InvalidQueryException("Missing closing bracket for in expression."); }
			return 1;
		}
		
		@Override
		public boolean validate(Token.TYPE previousTokenType) {
			return previousTokenType == null || previousTokenType == Token.TYPE.BRACKET_OPEN || previousTokenType == Token.TYPE.LOGICAL_OPERATOR || previousTokenType == Token.TYPE.LOGICAL_UNARY_OPERATOR;
		}
	}
	
	/**
	 * Logical Operator token handler
	 */
	private class LogicalOperatorTokenHandler extends TokenHandler {
		@Override
		public int _handleToken(ParseContext ctx) throws InvalidQueryException {
			Token token = ctx.getTokens()[ctx.getCurrentTokensIndex()];
			LogicalOperator logicalOp = LogicalOperatorFactory.createOperator(token.getValue(), ctx.getPrecedenceLevel());
			ctx.updateMaxPrecedence(logicalOp.getPrecedence());
			ctx.addExpression(LogicalExpressionFactory.createLogicalExpression(logicalOp));
			
			return 1;
		}
		
		@Override
		public boolean validate(Token.TYPE previousTokenType) {
			return previousTokenType == Token.TYPE.VALUE_OPERAND || previousTokenType == Token.TYPE.BRACKET_CLOSE;
		}
	}
	
	/**
	 * Logical Unary Operator token handler
	 */
	private class LogicalUnaryOperatorTokenHandler extends LogicalOperatorTokenHandler {
		@Override
		public boolean validate(Token.TYPE previousTokenType) {
			return previousTokenType == null || previousTokenType == Token.TYPE.BRACKET_OPEN || previousTokenType == Token.TYPE.LOGICAL_OPERATOR;
		}
	}
	
	/**
	 * Property Operand token handler
	 */
	private class PropertyOperandTokenHandler extends TokenHandler {
		@Override
		public int _handleToken(ParseContext ctx) throws InvalidQueryException {
			Token token = ctx.getTokens()[ctx.getCurrentTokensIndex()];
			ctx.getPrecedingExpression().setLeftOperand(token.getValue());
			
			return 1;
		}
		
		@Override
		public boolean validate(Token.TYPE previousTokenType) {
			return previousTokenType == Token.TYPE.RELATIONAL_OPERATOR || previousTokenType == Token.TYPE.RELATIONAL_OPERATOR_FUNC;
		}
	}
	
	/**
	 * Value Operand token handler
	 */
	private class ValueOperandTokenHandler extends TokenHandler {
		@Override
		public int _handleToken(ParseContext ctx) throws InvalidQueryException {
			Token token = ctx.getTokens()[ctx.getCurrentTokensIndex()];
			ctx.getPrecedingExpression().setRightOperand(token.getValue());
			
			return 1;
		}
		
		@Override
		public boolean validate(Token.TYPE previousTokenType) {
			return previousTokenType == Token.TYPE.PROPERTY_OPERAND;
		}
	}
}
