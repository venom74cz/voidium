import { useState } from 'react'
import { useTr } from '../i18n'
import { useToast } from './Toast'
import { api } from '../api'

interface Props {
  active: boolean
  onRefresh: () => void
}

export function MaintenanceBanner({ active, onRefresh }: Props) {
  const tr = useTr()
  const { toast } = useToast()
  const [toggling, setToggling] = useState(false)

  if (!active) return null

  const disable = async () => {
    setToggling(true)
    try {
      await api.action('maintenance_off')
      toast(tr('Maintenance mode disabled.', 'Režim údržby vypnut.'), 'success')
      onRefresh()
    } catch {
      toast(tr('Failed to disable maintenance.', 'Nepodařilo se vypnout údržbu.'), 'error')
    }
    setToggling(false)
  }

  return (
    <div className="maint-banner">
      <span>⚠ {tr('MAINTENANCE MODE ACTIVE', 'REŽIM ÚDRŽBY AKTIVNÍ')}</span>
      <button className="ghost" type="button" onClick={disable} disabled={toggling}>
        {toggling ? tr('Disabling...', 'Vypínám...') : tr('Disable', 'Vypnout')}
      </button>
    </div>
  )
}
