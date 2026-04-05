import { LoginForm } from "@/components/login-form"
import { Tv, ArrowRight } from "lucide-react"
import { Link } from "react-router-dom"

export default function LoginPage() {
  return (
    <div className="min-h-screen bg-black flex flex-col lg:flex-row overflow-hidden font-sans text-white">
      {/* Left Side: Login Form (Now first for natural reading) */}
      <div className="w-full lg:w-1/2 p-12 lg:p-24 flex flex-col justify-center items-center relative order-2 lg:order-1">
        {/* Glow effect for form focus */}
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[500px] h-[500px] bg-primary/5 blur-[120px] pointer-events-none rounded-full" />
        
        <div className="w-full max-w-sm relative z-10 animate-in fade-in slide-in-from-left-10 duration-700">
          <LoginForm />
        </div>
        
        {/* Footer info */}
        <div className="absolute bottom-12 left-1/2 -translate-x-1/2 w-full text-center text-[10px] font-black text-white/10 uppercase tracking-[0.3em] pointer-events-none">
          SECURE ENCRYPTION SHA-256 • BIDCAST GLOBAL NETWORK
        </div>
      </div>

      {/* Right Side: Brand & Impact (Now on the right) */}
      <div className="w-full lg:w-1/2 bg-primary p-12 lg:p-24 flex flex-col justify-between relative overflow-hidden order-1 lg:order-2">
        {/* Decorative pattern */}
        <div className="absolute inset-0 opacity-10 pointer-events-none" style={{ backgroundImage: 'radial-gradient(circle at 2px 2px, black 1px, transparent 0)', backgroundSize: '40px 40px' }} />
        <div className="absolute -bottom-20 -right-20 w-96 h-96 bg-black/5 rounded-full blur-3xl" />
        
        <div className="relative z-10 flex justify-end">
          <Link to="/" className="flex items-center gap-3 text-black group bg-black/5 px-4 py-2 rounded-2xl backdrop-blur-sm hover:bg-black hover:text-white transition-all">
            <span className="font-black tracking-tighter uppercase text-xs">Volver a la web</span>
            <ArrowRight size={18} />
          </Link>
        </div>

        <div className="relative z-10 text-right lg:text-left">
          <div className="h-32 w-32 md:h-48 md:w-48 bg-black text-primary flex items-center justify-center rounded-[2.5rem] shadow-2xl mb-12 ml-auto lg:ml-0 animate-in fade-in slide-in-from-right-10 duration-700">
            <Tv size={80} strokeWidth={2.5} />
          </div>
          <h2 className="text-6xl md:text-8xl font-black tracking-tighter text-black leading-[0.85] uppercase">
            DOMINA <br />EL AIRE.
          </h2>
          <p className="mt-8 text-xl md:text-2xl font-black text-black/60 uppercase tracking-tight leading-tight max-w-md ml-auto lg:ml-0">
            Gestiona tus campañas en tiempo real con la infraestructura más avanzada.
          </p>
        </div>

        <div className="relative z-10 flex items-center justify-end lg:justify-start gap-6 text-black">
           <p className="text-[10px] font-black uppercase tracking-widest">
              +2,400 ANUNCIANTES ACTIVOS
           </p>
           <div className="flex -space-x-3">
              {[1,2,3,4].map(i => (
                <div key={i} className="h-10 w-10 rounded-full border-4 border-primary bg-black flex items-center justify-center overflow-hidden">
                   <div className="h-full w-full bg-white/10" />
                </div>
              ))}
           </div>
        </div>
      </div>
    </div>
  )
}
