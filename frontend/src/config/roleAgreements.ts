export type AgreementRoleType = 'RUNNER' | 'GOODS_PUBLISHER' | 'SHOP_MERCHANT'
export type RoleActivationMode = 'AUTO_APPROVE' | 'PENDING_REVIEW'

const MODES: Record<AgreementRoleType, RoleActivationMode> = {
  RUNNER: 'AUTO_APPROVE',
  GOODS_PUBLISHER: 'AUTO_APPROVE',
  SHOP_MERCHANT: 'PENDING_REVIEW',
}

export function roleRequiresAgreement(roleType: string) {
  return Object.prototype.hasOwnProperty.call(MODES, roleType.trim().toUpperCase())
}

export function roleActivationMode(roleType: string): RoleActivationMode | null {
  if (!roleRequiresAgreement(roleType)) return null
  return MODES[roleType.trim().toUpperCase() as AgreementRoleType]
}
