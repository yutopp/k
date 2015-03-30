package org.kframework.kore

import scala.collection.JavaConverters._

import org.kframework.definition.Module

/**
 * Created by dwightguth on 3/27/15.
 */
object Assoc extends {
  def flatten(k: KApply, m: Module): java.util.List[K] = {
    if (m.attributesFor(k.klabel).contains("assoc")) {
      return flatten(k.klabel, k.klist.items.asScala, m).asJava
    }
    return List[K](k).asJava
  }

  private def flatten(label: KLabel, list: Seq[K], m: Module): Seq[K] = {
    list flatMap { case k: KApply =>
        if (k.klabel == label)
          flatten(label, k.klist.items.asScala, m)
        else if (k.klabel == m.attributesFor(k.klabel).get[KLabel]("unit"))
          List()
        else
          List(k)
      case other => List(other)
    }
  }
}
