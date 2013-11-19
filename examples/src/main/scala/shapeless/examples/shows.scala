/*
 * Copyright (c) 2013 Lars Hupel
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
 */

package shapeless.examples

import shapeless._

sealed trait Super
case class Foo(i : Int, s : String) extends Super
case class Bar(i : Int) extends Super
case class BarRec(i : Int, rec: Super) extends Super

object Super {
  implicit val instance = TypeClass[Show, Super]
}

sealed trait MutualA
case class MutualA1(x: Int) extends MutualA
case class MutualA2(b: MutualB) extends MutualA

object MutualA {
  implicit val aInstance: Show[MutualA] = TypeClass[Show, MutualA]
}

sealed trait MutualB
case class MutualB1(x: Int) extends MutualB
case class MutualB2(b: MutualA) extends MutualB

object MutualB {
  implicit val bInstance: Show[MutualB] = TypeClass[Show, MutualB]
}

object ShowExamples extends App {
  import ShowSyntax._

  val bar: Super = Bar(0)
  val rec: Super = BarRec(1, Foo(0, "foo"))

  assert(bar.show == "Bar(i = 0)")
  assert(rec.show == "BarRec(i = 1, rec = Foo(i = 0, s = foo))")

  val mutual: MutualA = MutualA2(MutualB2(MutualA1(0)))

  assert(mutual.show == "MutualA2(b = MutualB2(b = MutualA1(x = 0)))")
}

trait ShowSyntax {
  def show : String
}

object ShowSyntax {
  implicit def showSyntax[T](a : T)(implicit st : Show[T]) : ShowSyntax = new ShowSyntax {
    def show = st.show(a)
  }
}

trait Show[T] {
  def show(t: T): String
}

object Show extends TypeClassCompanion[Show] {
  implicit def stringShow: Show[String] = new Show[String] {
    def show(t: String) = t
  }

  implicit def intShow: Show[Int] = new Show[Int] {
    def show(n: Int) = n.toString
  }

  implicit def showInstance: TypeClass[Show] = new TypeClass[Show] {
    def emptyProduct = new Show[HNil] {
      def show(t: HNil) = ""
    }

    def product[F, T <: HList](FHead : Show[F], FTail : Show[T]) = new Show[F :: T] {
      def show(ft: F :: T) = s"${FHead.show(ft.head)} :: ${FTail.show(ft.tail)}"
    }

    override def namedProduct[F, T <: HList](FHead : Show[F], name : String, FTail : Show[T]) = new Show[F :: T] {
      def show(ft: F :: T) = {
        val head = FHead.show(ft.head)
        val tail = FTail.show(ft.tail)
        if (tail.isEmpty)
          s"$name = $head"
        else
          s"$name = $head, $tail"
      }
    }

    override def coproduct1[L](CL: => Show[L]) = new Show[L :+: CNil] {
      def show(l: L :+: CNil) = l match {
        case Inl(l) => s"Inl(${CL.show(l)})"
        case Inr(_) => sys.error("absurd")
      }
    }

    override def namedCoproduct1[L](CL: => Show[L], name: String) = new Show[L :+: CNil] {
      def show(l: L :+: CNil) = l match {
        case Inl(l) => s"$name(${CL.show(l)})"
        case Inr(_) => sys.error("absurd")
      }
    }

    def coproduct[L, R <: Coproduct](CL: => Show[L], CR: => Show[R]) = new Show[L :+: R] {
      def show(lr: L :+: R) = lr match {
        case Inl(l) => s"Inl(${CL.show(l)})"
        case Inr(r) => s"Inr(${CR.show(r)})"
      }
    }

    override def namedCoproduct[L, R <: Coproduct](CL: => Show[L], name: String, CR: => Show[R]) = new Show[L :+: R] {
      def show(lr: L :+: R) = lr match {
        case Inl(l) => s"$name(${CL.show(l)})"
        case Inr(r) => s"${CR.show(r)}"
      }
    }

    override def namedField[F](instance: Show[F], name: String) = new Show[F] {
      def show(f: F) = s"$name = ${instance.show(f)}"
    }

    override def namedCase[F](instance: Show[F], name: String) = new Show[F] {
      def show(f: F) = s"$name(${instance.show(f)})"
    }

    def project[F, G](instance : => Show[G], to : F => G, from : G => F) = new Show[F] {
      def show(f: F) = instance.show(to(f))
    }
  }
}

// vim: expandtab:ts=2:sw=2
