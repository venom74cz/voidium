import { useTr } from '../i18n'

interface Props {
  title: string
  titleCz: string
  description?: string
  descriptionCz?: string
  children?: React.ReactNode
}

export function PageHeader({ title, titleCz, description, descriptionCz, children }: Props) {
  const tr = useTr()
  return (
    <header className="page-header">
      <div className="page-header-text">
        <h1>{tr(title, titleCz)}</h1>
        {description && <p>{tr(description, descriptionCz || description)}</p>}
      </div>
      {children && <div className="page-header-actions">{children}</div>}
    </header>
  )
}
