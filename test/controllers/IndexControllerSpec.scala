/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers

import base.SpecBase
import connectors.SubmissionDraftConnector
import models.Status.Completed
import models.UserAnswers
import models.registration.Matched.Success
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.{reset, verify, when}
import pages.TrustDetailsStatus
import pages.register.ExistingTrustMatched
import play.api.inject.bind
import play.api.libs.json.JsString
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.FeatureFlagService

import scala.concurrent.Future

class IndexControllerSpec extends SpecBase {

  private val featureFlagService: FeatureFlagService = mock[FeatureFlagService]
  private val submissionDraftConnector: SubmissionDraftConnector = mock[SubmissionDraftConnector]

  "Index Controller" when {

    "pre-existing user answers" when {

      "trust details completed" must {
        "redirect to CheckDetailsController" in {

          reset(registrationsRepository)

          val answers = emptyUserAnswers
            .set(TrustDetailsStatus, Completed).success.value

          val application = applicationBuilder()
            .overrides(bind[FeatureFlagService].toInstance(featureFlagService))
            .build()

          when(registrationsRepository.get(any())(any())).thenReturn(Future.successful(Some(answers)))
          when(registrationsRepository.set(any())(any(), any())).thenReturn(Future.successful(true))
          when(featureFlagService.is5mldEnabled()(any(), any())).thenReturn(Future.successful(false))

          val request = FakeRequest(GET, routes.IndexController.onPageLoad(fakeDraftId).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustBe controllers.register.trust_details.routes.CheckDetailsController.onPageLoad(fakeDraftId).url

          application.stop()
        }
      }

      "trust details in progress or not started" when {

        "trust has been matched" must {
          "redirect to WhenTrustSetupController" in {

            reset(registrationsRepository)

            val answers = emptyUserAnswers
              .setAtPath(ExistingTrustMatched.path, JsString(Success.toString)).success.value

            val application = applicationBuilder()
              .overrides(bind[FeatureFlagService].toInstance(featureFlagService))
              .build()

            when(registrationsRepository.get(any())(any())).thenReturn(Future.successful(Some(emptyUserAnswers)))
            when(registrationsRepository.getMainAnswers(any())(any())).thenReturn(Future.successful(Some(answers)))
            when(registrationsRepository.set(any())(any(), any())).thenReturn(Future.successful(true))
            when(featureFlagService.is5mldEnabled()(any(), any())).thenReturn(Future.successful(false))

            val request = FakeRequest(GET, routes.IndexController.onPageLoad(fakeDraftId).url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustBe controllers.register.trust_details.routes.WhenTrustSetupController.onPageLoad(fakeDraftId).url

            application.stop()
          }
        }

        "trust has not been matched" must {
          "redirect to TrustNameController" in {

            reset(registrationsRepository)

            val application = applicationBuilder()
              .overrides(bind[FeatureFlagService].toInstance(featureFlagService))
              .build()

            when(registrationsRepository.get(any())(any())).thenReturn(Future.successful(Some(emptyUserAnswers)))
            when(registrationsRepository.getMainAnswers(any())(any())).thenReturn(Future.successful(Some(emptyUserAnswers)))
            when(registrationsRepository.set(any())(any(), any())).thenReturn(Future.successful(true))
            when(featureFlagService.is5mldEnabled()(any(), any())).thenReturn(Future.successful(true))

            val request = FakeRequest(GET, routes.IndexController.onPageLoad(fakeDraftId).url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustBe controllers.register.trust_details.routes.TrustNameController.onPageLoad(fakeDraftId).url

            application.stop()
          }
        }
      }

      "update value of is5mldEnabled and isTaxable to true in user answers" in {

        reset(registrationsRepository)

        val userAnswers = emptyUserAnswers.copy(is5mldEnabled = false)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[FeatureFlagService].toInstance(featureFlagService))
          .build()

        when(registrationsRepository.get(any())(any())).thenReturn(Future.successful(Some(userAnswers)))
        when(registrationsRepository.getMainAnswers(any())(any())).thenReturn(Future.successful(Some(emptyUserAnswers)))
        when(registrationsRepository.set(any())(any(), any())).thenReturn(Future.successful(true))
        when(featureFlagService.is5mldEnabled()(any(), any())).thenReturn(Future.successful(true))
        when(submissionDraftConnector.getIsTrustTaxable(any())(any(), any())).thenReturn(Future.successful(true))

        val request = FakeRequest(GET, routes.IndexController.onPageLoad(fakeDraftId).url)

        route(application, request).value.map { _ =>
          val uaCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(registrationsRepository).set(uaCaptor.capture)(any(), any())

          uaCaptor.getValue.is5mldEnabled mustBe true
          uaCaptor.getValue.isTaxable mustBe true

          application.stop()
        }
      }

      "update value of is5mldEnabled and isTaxable to false in user answers" in {

        reset(registrationsRepository)

        val userAnswers = emptyUserAnswers.copy(is5mldEnabled = false)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[FeatureFlagService].toInstance(featureFlagService))
          .build()

        when(registrationsRepository.get(any())(any())).thenReturn(Future.successful(Some(userAnswers)))
        when(registrationsRepository.getMainAnswers(any())(any())).thenReturn(Future.successful(Some(emptyUserAnswers)))
        when(registrationsRepository.set(any())(any(), any())).thenReturn(Future.successful(true))
        when(featureFlagService.is5mldEnabled()(any(), any())).thenReturn(Future.successful(false))
        when(submissionDraftConnector.getIsTrustTaxable(any())(any(), any())).thenReturn(Future.successful(false))

        val request = FakeRequest(GET, routes.IndexController.onPageLoad(fakeDraftId).url)

        route(application, request).value.map { _ =>
          val uaCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(registrationsRepository).set(uaCaptor.capture)(any(), any())

          uaCaptor.getValue.is5mldEnabled mustBe false
          uaCaptor.getValue.isTaxable mustBe false

          application.stop()
        }
      }
    }

    "no pre-existing user answers" must {

      "redirect to WhenTrustSetupController" when {
        "trust has been matched" in {

          reset(registrationsRepository)

          val answers = emptyUserAnswers
            .setAtPath(ExistingTrustMatched.path, JsString(Success.toString)).success.value

          val application = applicationBuilder()
            .overrides(bind[FeatureFlagService].toInstance(featureFlagService))
            .build()

          when(registrationsRepository.get(any())(any())).thenReturn(Future.successful(None))
          when(registrationsRepository.getMainAnswers(any())(any())).thenReturn(Future.successful(Some(answers)))
          when(registrationsRepository.set(any())(any(), any())).thenReturn(Future.successful(true))
          when(featureFlagService.is5mldEnabled()(any(), any())).thenReturn(Future.successful(false))

          val request = FakeRequest(GET, routes.IndexController.onPageLoad(fakeDraftId).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustBe controllers.register.trust_details.routes.WhenTrustSetupController.onPageLoad(fakeDraftId).url

          application.stop()
        }
      }

      "redirect to TrustNameController" when {
        "trust has not been matched" in {

          reset(registrationsRepository)

          val application = applicationBuilder()
            .overrides(bind[FeatureFlagService].toInstance(featureFlagService))
            .build()

          when(registrationsRepository.get(any())(any())).thenReturn(Future.successful(None))
          when(registrationsRepository.getMainAnswers(any())(any())).thenReturn(Future.successful(Some(emptyUserAnswers)))
          when(registrationsRepository.set(any())(any(), any())).thenReturn(Future.successful(true))
          when(featureFlagService.is5mldEnabled()(any(), any())).thenReturn(Future.successful(true))

          val request = FakeRequest(GET, routes.IndexController.onPageLoad(fakeDraftId).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustBe controllers.register.trust_details.routes.TrustNameController.onPageLoad(fakeDraftId).url

          application.stop()
        }
      }

      "instantiate new set of user answers" when {

        "5mld enabled" must {
          "add is5mldEnabled = true to user answers" in {

            reset(registrationsRepository)

            val application = applicationBuilder(userAnswers = None)
              .overrides(bind[FeatureFlagService].toInstance(featureFlagService))
              .build()

            when(registrationsRepository.get(any())(any())).thenReturn(Future.successful(None))
            when(registrationsRepository.getMainAnswers(any())(any())).thenReturn(Future.successful(Some(emptyUserAnswers)))
            when(registrationsRepository.set(any())(any(), any())).thenReturn(Future.successful(true))
            when(featureFlagService.is5mldEnabled()(any(), any())).thenReturn(Future.successful(true))

            val request = FakeRequest(GET, routes.IndexController.onPageLoad(fakeDraftId).url)

            route(application, request).value.map { _ =>
              val uaCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])
              verify(registrationsRepository).set(uaCaptor.capture)(any(), any())

              uaCaptor.getValue.is5mldEnabled mustBe true
              uaCaptor.getValue.draftId mustBe fakeDraftId
              uaCaptor.getValue.internalAuthId mustBe "internalId"

              application.stop()
            }
          }
        }

        "5mld not enabled" must {
          "add is5mldEnabled = false to user answers" in {

            reset(registrationsRepository)

            val application = applicationBuilder(userAnswers = None)
              .overrides(bind[FeatureFlagService].toInstance(featureFlagService))
              .build()

            when(registrationsRepository.get(any())(any())).thenReturn(Future.successful(None))
            when(registrationsRepository.getMainAnswers(any())(any())).thenReturn(Future.successful(Some(emptyUserAnswers)))
            when(registrationsRepository.set(any())(any(), any())).thenReturn(Future.successful(true))
            when(featureFlagService.is5mldEnabled()(any(), any())).thenReturn(Future.successful(false))

            val request = FakeRequest(GET, routes.IndexController.onPageLoad(fakeDraftId).url)

            route(application, request).value.map { _ =>
              val uaCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])
              verify(registrationsRepository).set(uaCaptor.capture)(any(), any())

              uaCaptor.getValue.is5mldEnabled mustBe false
              uaCaptor.getValue.draftId mustBe fakeDraftId
              uaCaptor.getValue.internalAuthId mustBe "internalId"

              application.stop()
            }
          }
        }
      }
    }
  }
}
