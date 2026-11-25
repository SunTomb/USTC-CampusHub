const active = new Set();
function hold(owner) { active.add(owner); wx.hideTabBar({ animation: false, fail() {} }); }
function release(owner) { active.delete(owner); if (!active.size) wx.showTabBar({ animation: false, fail() {} }); }
module.exports = { hold, release };
