import { Link } from 'react-router-dom';
import { Layers } from 'lucide-react';
export function Brand() { return <Link to="/" className="brand" aria-label="SmartEdu — главная"><span className="brand-mark"><Layers size={23}/></span><span>Smart<span className="brand-light">Edu</span></span></Link>; }
