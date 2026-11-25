function id(value) {
  const n = Number(value);
  if (!/^[1-9]\d*$/.test(String(value)) || !Number.isSafeInteger(n)) throw new Error('闲置或买家编号无效');
  return n;
}
function createGoodsApi(client) {
  const listing = value => '/goods/' + id(value);
  return {
    goods: () => client.get('/goods'),
    goodsDetail: value => client.get(listing(value)),
    goodsCategories: () => client.get('/categories'),
    publishGoods: data => client.post('/goods', data),
    updateGoods: (value, data) => client.put(listing(value), data),
    deleteGoods: value => client.delete(listing(value)),
    offShelfGoods: value => client.post(listing(value) + '/off-shelf', {}),
    goodsIntents: value => client.get(listing(value) + '/intents'),
    createGoodsIntent: (value, message) => client.post(listing(value) + '/intents', { message }),
    markGoodsSold: (value, buyerId) => client.post(listing(value) + '/mark-sold', { buyerId: id(buyerId) }),
  };
}
module.exports = { createGoodsApi };
