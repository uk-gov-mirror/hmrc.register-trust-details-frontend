/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.register.trust_details

import controllers.actions.register.{DraftIdRetrievalActionProvider, RegistrationDataRequiredAction, RegistrationIdentifierAction}
import forms.YesNoFormProvider
import javax.inject.Inject
import navigation.Navigator
import pages.trust_details.InheritanceTaxActPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.RegistrationsRepository
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import views.html.register.trust_details.InheritanceTaxActView

import scala.concurrent.{ExecutionContext, Future}

class InheritanceTaxActController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             registrationsRepository: RegistrationsRepository,
                                             navigator: Navigator,
                                             identify: RegistrationIdentifierAction,
                                             getData: DraftIdRetrievalActionProvider,
                                             requireData: RegistrationDataRequiredAction,
                                             yesNoFormProvider: YesNoFormProvider,
                                             val controllerComponents: MessagesControllerComponents,
                                             view: InheritanceTaxActView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private def actions(draftId: String) = identify andThen getData(draftId) andThen requireData

  val form: Form[Boolean] = yesNoFormProvider.withPrefix("inheritanceTaxAct")

  def onPageLoad(draftId: String): Action[AnyContent] = actions(draftId) {
    implicit request =>

      val preparedForm = request.userAnswers.get(InheritanceTaxActPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, draftId))
  }

  def onSubmit(draftId: String) = actions(draftId).async {
    implicit request =>

      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          Future.successful(BadRequest(view(formWithErrors, draftId))),

        value => {
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(InheritanceTaxActPage, value))
            _              <- registrationsRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(InheritanceTaxActPage, draftId, updatedAnswers))
        }
      )
  }
}
