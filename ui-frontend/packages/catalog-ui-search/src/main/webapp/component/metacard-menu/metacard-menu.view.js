/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

const wreqr = require('../../js/wreqr.js')
const Marionette = require('marionette')
const Backbone = require('backbone')
const $ = require('jquery')
const template = require('./metacard-menu.hbs')
const CustomElements = require('../../js/CustomElements.js')
const store = require('../../js/store.js')
const metacardInstance = require('../metacard/metacard.js')
const MetacardTitleView = require('../metacard-title/metacard-title.view.js')

module.exports = Marionette.LayoutView.extend({
  template,
  tagName: CustomElements.register('metacard-menu'),
  events: {
    'click > .workspace-title': 'goToWorkspace',
  },
  regions: {
    metacardTitle: '.metacard-title',
  },
  onFirstRender() {
    this.listenTo(metacardInstance, 'change:currentMetacard', this.render)
  },
  onRender() {
    if (metacardInstance.get('currentMetacard')) {
      this.metacardTitle.show(
        new MetacardTitleView({
          model: new Backbone.Collection(
            metacardInstance.get('currentMetacard')
          ),
        })
      )
    }
  },
  goToWorkspace(e) {
    const workspaceId = $(e.currentTarget).attr('data-workspaceid')
    wreqr.vent.trigger('router:navigate', {
      fragment: 'workspaces/' + workspaceId,
      options: {
        trigger: true,
      },
    })
  },
  serializeData() {
    const currentWorkspace = store.getCurrentWorkspace()
    let resultJSON, workspaceJSON
    if (metacardInstance.get('currentMetacard')) {
      resultJSON = metacardInstance.get('currentMetacard').toJSON()
    }
    if (currentWorkspace) {
      workspaceJSON = currentWorkspace.toJSON()
    }
    return {
      result: resultJSON,
      workspace: workspaceJSON,
    }
  },
})
