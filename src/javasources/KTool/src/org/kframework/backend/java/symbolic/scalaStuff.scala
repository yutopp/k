// Copyright (c) 2014 K Team. All Rights Reserved.
package org.kframework.backend.java.symbolic;

object TruthValue extends Enumeration {
  type TruthValue = Value
  val True, False, Unknown = Value

  def and(a: TruthValue, b: TruthValue) = (a, b) match {
    case (True, True) => True
    case (False, _) => False
    case (_, False) => False
    case _ => Unknown
  }

  def or(a: TruthValue, b: TruthValue) = (a, b) match {
    case (False, False) => False
    case (True, _) => True
    case (_, True) => True
    case _ => Unknown
  }
}
import TruthValue._

trait Term
trait Variable

object SymbolicConstraint {
  trait Equality extends SymbolicConstraint {
    def truthValue = Unknown
  }

  import java.util.Set
  import scala.collection._
  import JavaConversions._

  case class And(symbolicConstraints: Set[SymbolicConstraint]) extends SymbolicConstraint {
    def truthValue = symbolicConstraints map { _.truthValue } reduce TruthValue.and
  }

  case class Or(symbolicConstraints: Set[SymbolicConstraint]) extends SymbolicConstraint {
    def truthValue = symbolicConstraints map { _.truthValue } reduce TruthValue.or
  }
  case class ComplexEquality(lhs: Term, rhs: Term) extends SymbolicConstraint with Equality
  case class SubstitutionEquality(lhs: Variable, rhs: Term) extends SymbolicConstraint with Equality

  def conjunctionToDisjunction(c: And): Or = {
    val ors = c.symbolicConstraints map { _.asInstanceOf[Or] } 
    ors reduce bla
  }

  def bla(a: Or, b: Or): Or = Or(
    a.symbolicConstraints flatMap { elementOfA =>
      b.symbolicConstraints map { elementOfB =>
        And(immutable.Set(elementOfA, elementOfB)).asInstanceOf[SymbolicConstraint]
      }
    })
}
trait SymbolicConstraint {
  def truthValue: TruthValue
}
