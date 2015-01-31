package org.kframework.kore

import org.kframework.kore
import org.kframework.attributes._
import org.kframework.koreimplementation.{KApplyToString, KTokenToString}
import collection.JavaConverters._

object ADT {

  case class KLabel(name: String) extends kore.KLabel {
    override def toString = name
  }

  case class KApply[KK <: K](klabel: kore.KLabel, klist: kore.KList, att: Att = Att()) extends kore.KApply
    with KApplyToString

  case class KSequence(elements: List[K], att: Att = Att()) extends kore.KSequence {
    def items: java.util.List[K] = elements.asJava
  }

  case class KVariable(name: String, att: Att = Att()) extends kore.KVariable

  case class Sort(name: String) extends kore.Sort {
    override def toString = name
  }

  case class KToken(sort: kore.Sort, s: String, att: Att = Att()) extends kore.KToken with KTokenToString

  case class KList(elements: List[K]) extends kore.KList {
    def items: java.util.List[K] = elements.asJava
  }

  case class KRewrite(left: kore.K, right: kore.K, att: Att = Att()) extends kore.KRewrite

  case class InjectedKLabel(klabel: kore.KLabel, att: Att) extends kore.InjectedKLabel

}