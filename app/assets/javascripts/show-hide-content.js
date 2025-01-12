// From govuk_frontend_toolkit
// Placed here until assets frontend is updated

(function (global) {
  'use strict';

  var $ = global.jQuery;
  var GOVUK = global.GOVUK || {};

  function ShowHideContent () {
    var self = this;

    // Radio and Checkbox selectors
    var selectors = {
      namespace: 'ShowHideContent',
      radio: '.block-label[data-target] input[type="radio"]',
      checkbox: '.block-label[data-target] input[type="checkbox"]'
    }

    // Escape name attribute for use in DOM selector
    function escapeElementName (str) {
      var result;
      result = str.replace('[', '\[').replace(']', '\]');
      return result;
    }

    // Adds ARIA attributes to control + associated content
    function initToggledContent () {
      var $control = $(this);
      var $content = getToggledContent($control);

      // Set aria-controls and defaults
      if ($content.length) {
        $control.attr('aria-controls', $content.attr('id'));
        $control.attr('aria-expanded', 'false');
        $content.attr('aria-hidden', 'true');
      }
    }

    // Return toggled content for control
    function getToggledContent ($control) {
      var id = $control.attr('aria-controls');

      // ARIA attributes aren't set before init
      if (!id) {
        id = $control.closest('label').data('target');
      }

      // Find show/hide content by id
      return $('#' + id);
    }

    // Show toggled content for control
    function showToggledContent ($control, $content) {
      // Show content
      if ($content.attr('aria-hidden') == 'true') {
        $content.removeClass('js-hidden');
        $content.attr('aria-hidden', 'false');
      }

      // If the controlling input, update aria-expanded
      getRelatedControls($control).each(function () {
        if ($(this).attr('aria-controls') == $content.attr('id')) {
          $(this).attr('aria-expanded', 'true');
        }
      });
    }

    function getRelatedControls ($control) {
      return $('[aria-controls="' + $control.attr('aria-controls') + '"]');
    }

    function shouldContentBeVisible ($control) {
      // takes a current control and determines if the content related should be visible
      // i.e. checks to see if another related control is selected
      // this allows us to prevent hiding content before showing it again, triggering an unneeded aria response
      return getRelatedControls($control).filter(':checked').length > 0;

    }

    // Hide toggled content for control
    function hideToggledContent ($control, $content) {
      $content = $content || getToggledContent($control);
      // If the controlling input, update aria-expanded
      if ($control.attr('aria-controls')) {
        $control.attr('aria-expanded', 'false');
      }
      // Hide content (only if we need to)
      if ($content.attr('aria-hidden') == 'false' && !shouldContentBeVisible($control)) {
        $content.addClass('js-hidden');
        $content.attr('aria-hidden', 'true');
      }

    }

    // Handle radio show/hide
    function handleRadioContent ($control, $content) {
      // All radios in this group which control content
      var selector, $radios;
      selector = selectors.radio + '[name=' + escapeElementName($control.attr('name')) + '][aria-controls]';
      $radios = $control.closest('form').find(selector);

      // Hide content for radios in group
      $radios.each(function () {
        hideToggledContent($(this));
      })

      // Select content for this control
      if ($control.is('[aria-controls]')) {
        showToggledContent($control, $content);
      }
    }

    // Handle checkbox show/hide
    function handleCheckboxContent ($control, $content) {
      // Show checkbox content
      if ($control.is(':checked')) {
        showToggledContent($control, $content);
      } else { // Hide checkbox content

        //update related checkboxes
        // If the controlling input, update aria-expanded if no other checkboxes pointing to this content are checked
        if(!shouldContentBeVisible($control)){
          hideToggledContent($control, $content);
          getRelatedControls($control).each(function () {
            if ($(this).attr('aria-controls') == $content.attr('id')) {
              $(this).attr('aria-expanded', 'false');
            }
          });
        }
      }
    }

    // Set up event handlers etc
    function init ($container, elementSelector, eventSelectors, handler) {
      $container = $container || $(document.body);

      // Handle control clicks
      function deferred () {
        var $control = $(this);
        handler($control, getToggledContent($control));
      }

      // Prepare ARIA attributes
      var $controls = $(elementSelector);
      $controls.each(initToggledContent);

      // Handle events
      $.each(eventSelectors, function (idx, eventSelector) {
        $container.on('click.' + selectors.namespace, eventSelector, deferred);
      })

      // Any already :checked on init?
      if ($controls.is(':checked')) {
        $controls.filter(':checked').each(deferred);
      }
    }

    // Get event selectors for all radio groups
    function getEventSelectorsForRadioGroups () {
      var radioGroups = [];

      // Build an array of radio group selectors
      return $(selectors.radio).map(function () {
        var groupName = $(this).attr('name');

        if ($.inArray(groupName, radioGroups) === -1) {
          radioGroups.push(groupName);
          return 'input[type="radio"][name="' + $(this).attr('name') + '"]';
        }
        return null;
      })
    }

    // Set up radio show/hide content for container
    self.showHideRadioToggledContent = function ($container) {
      init($container, selectors.radio, getEventSelectorsForRadioGroups(), handleRadioContent);
    }

    // Set up checkbox show/hide content for container
    self.showHideCheckboxToggledContent = function ($container) {
      init($container, selectors.checkbox, [selectors.checkbox], handleCheckboxContent);
    }

    // Remove event handlers
    self.destroy = function ($container) {
      $container = $container || $(document.body);
      $container.off('.' + selectors.namespace);
    }
  }

  ShowHideContent.prototype.init = function ($container) {
    this.showHideRadioToggledContent($container);
    this.showHideCheckboxToggledContent($container);
  }

  GOVUK.ShowHideContent = ShowHideContent;
  global.GOVUK = GOVUK;
})(window);
