import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
  FieldSeparator,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { ArrowRight } from "lucide-react"
import { Link } from "react-router-dom"

export function LoginForm({
  className,
  ...props
}: React.ComponentProps<"form">) {
  return (
    <form className={cn("flex flex-col gap-8 w-full max-w-sm", className)} {...props}>
      <div className="flex flex-col gap-2">
        <h1 className="text-4xl font-black tracking-tighter text-white uppercase leading-none">BIENVENIDO.</h1>
        <p className="text-sm font-medium text-white/40">
          Ingresa tus credenciales para acceder al panel de control de BidCast.
        </p>
      </div>
      
      <FieldGroup className="gap-4">
        <Field>
          <FieldLabel htmlFor="email" className="text-white/50 font-black uppercase text-[10px] tracking-[0.2em] mb-2 block">Email</FieldLabel>
          <Input
            id="email"
            type="email"
            placeholder="admin@bidcast.tech"
            required
            className="bg-white/5 border-white/10 text-white placeholder:text-white/20 h-14 rounded-2xl focus:border-primary focus:ring-1 focus:ring-primary transition-all text-base"
          />
        </Field>
        <Field>
          <div className="flex items-center justify-between mb-2">
            <FieldLabel htmlFor="password" className="text-white/50 font-black uppercase text-[10px] tracking-[0.2em] block">Contraseña</FieldLabel>
            <a
              href="#"
              className="text-[10px] font-black uppercase tracking-[0.2em] text-primary hover:text-primary/80 transition-colors"
            >
              ¿Olvidaste tu clave?
            </a>
          </div>
          <Input
            id="password"
            type="password"
            required
            className="bg-white/5 border-white/10 text-white h-14 rounded-2xl focus:border-primary focus:ring-1 focus:ring-primary transition-all text-base"
          />
        </Field>
        
        <div className="pt-4 flex flex-col gap-4">
          <Button type="submit" className="w-full bg-primary text-black font-black h-14 text-lg hover:bg-primary/90 shadow-[0_10px_40px_rgba(255,247,46,0.15)] rounded-2xl">
            ENTRAR AHORA <ArrowRight className="ml-2 h-5 w-5" />
          </Button>
          
          <FieldSeparator className="text-[10px] font-black uppercase tracking-[0.3em] text-white/10 py-2">O accede con</FieldSeparator>
          
          <Button variant="outline" type="button" className="w-full border-white/10 text-white hover:bg-white/5 h-14 font-black rounded-2xl tracking-widest text-xs">
            GITHUB ACCOUNT
          </Button>
        </div>

        <FieldDescription className="text-center mt-8 font-bold text-white/20 uppercase tracking-widest text-[10px]">
          ¿No tienes cuenta?{" "}
          <Link to="/" className="text-white hover:text-primary transition-colors underline underline-offset-4">
            Regístrate aquí
          </Link>
        </FieldDescription>
      </FieldGroup>
    </form>
  )
}
