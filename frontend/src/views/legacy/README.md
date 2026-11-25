# Preview-only financial modules

These components preserve the pre-closure wallet and escrow implementation. They are **not deleted**, but are mounted only when the backend `/api/platform/features` explicitly enables financial operations. The default is closed; missing/unavailable configuration also stays closed.

- `WalletWorkspace.vue`: preserved wallet workspace.
- `GoodsEscrowDetail.vue`: preserved goods escrow UI.
- `AdminWalletWorkspace.vue`: preserved administrative financial actions.

Changing only a frontend flag cannot enable backend writes. Role deposits are not part of the new agreement-based identity flow, even if other money functions are re-enabled later. Reopening requires separate product/operational approval and validation of historical financial records.
