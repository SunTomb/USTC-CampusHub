const { zones } = require('../../lib/display');
Component({ options: { styleIsolation: 'apply-shared' }, properties: { value: { type: String, value: '' }, includeAll: { type: Boolean, value: true } }, data: { zones }, methods: { change(event) { this.triggerEvent('change', { value: event.currentTarget.dataset.value }); } } });
